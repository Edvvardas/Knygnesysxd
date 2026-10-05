package lt.prifkodas.knygnesys.stats.service;

import lombok.RequiredArgsConstructor;
import lt.prifkodas.knygnesys.progress.ReadingProgress;
import lt.prifkodas.knygnesys.progress.repository.ReadingProgressRepository;
import lt.prifkodas.knygnesys.readinglist.ReadingList;
import lt.prifkodas.knygnesys.readinglist.repository.ReadingListRepository;
import lt.prifkodas.knygnesys.stats.dto.GenreDataPoint;
import lt.prifkodas.knygnesys.stats.dto.PagesDayDataPoint;
import lt.prifkodas.knygnesys.stats.dto.StatsDataPoint;
import lt.prifkodas.knygnesys.stats.dto.StatsResponse;
import lt.prifkodas.knygnesys.stats.dto.StatsSummary;
import lt.prifkodas.knygnesys.user.User;
import lt.prifkodas.knygnesys.user.service.UserService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * REFAKTORINGO PASIŪLYMAS (kodo peržiūros / statinės analizės rezultatas).
 * Lyginant su originalu:
 *  - laikotarpių pavadinimai iškelti į konstantas (PMD AvoidDuplicateLiterals, Sonar S1192);
 *  - "kibirų" (buckets) kūrimas iškeltas į dateBuckets()/hourBuckets() (DRY: 6 beveik vienodi while ciklai -> 1);
 *  - įrašų skaičiavimas iškeltas į countEntries()/findBucket()/isWithin() (DRY: 2 dubliuoti for ciklai -> 1);
 *  - getPagesPerDay() vidinė logika iškelta į addPagesReadForBook() (Cognitive 16 -> < 15);
 *  - switch'ai turi default šaką (Sonar S131, SpotBugs SF_SWITCH_NO_DEFAULT);
 *  - toUpperCase naudoja Locale.ROOT (SpotBugs DM_CONVERT_CASE), Locale.of vietoj pasenusio konstruktoriaus.
 * Elgsena nepakitusi: visi 15 StatsServiceTest testų praeina be pakeitimų.
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final String DAY = "DAY";
    private static final String WEEK = "WEEK";
    private static final String MONTH = "MONTH";
    private static final String YEAR = "YEAR";
    private static final Set<String> PERIODS = Set.of(DAY, WEEK, MONTH, YEAR);

    private static final Locale LOCALE_LT = Locale.of("lt", "LT");
    private static final DateTimeFormatter DAY_LABEL_FMT   = DateTimeFormatter.ofPattern("d MMM", LOCALE_LT);
    private static final DateTimeFormatter MONTH_LABEL_FMT = DateTimeFormatter.ofPattern("MMM yyyy", LOCALE_LT);

    private final ReadingListRepository readingListRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final UserService userService;

    public StatsResponse getStats(String username, String period, String date, String startDate, String endDate) {
        String normalizedPeriod = period.toUpperCase(Locale.ROOT);
        if (!PERIODS.contains(normalizedPeriod)) {
            throw new IllegalArgumentException("Netinkamas laikotarpis: " + period);
        }

        User user = userService.getByUsername(username);
        List<ReadingList> allEntries = readingListRepository.findAllByUserId(user.getId());

        StatsSummary summary = buildSummary(allEntries);

        if (startDate != null && endDate != null) {
            LocalDateTime since = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime anchor = LocalDate.parse(endDate).atTime(23, 59, 59);
            if (anchor.isBefore(since)) {
                throw new IllegalArgumentException("Netinkamas intervalas: endDate yra prieš startDate");
            }

            List<ReadingList> filteredEntries = filterByRange(allEntries, since, anchor);
            List<StatsDataPoint> dataPoints = buildDataPointsForRange(filteredEntries, since, anchor, normalizedPeriod);
            return new StatsResponse(summary, normalizedPeriod, dataPoints);
        }

        LocalDateTime anchor = date != null
                ? LocalDate.parse(date).atTime(23, 59, 59)
                : LocalDateTime.now();
        LocalDateTime since = calculateSince(anchor, normalizedPeriod);

        List<ReadingList> filteredEntries = filterByRange(allEntries, since, anchor);

        List<StatsDataPoint> dataPoints = buildDataPoints(filteredEntries, anchor, since, normalizedPeriod);

        return new StatsResponse(summary, normalizedPeriod, dataPoints);
    }

    public List<PagesDayDataPoint> getPagesPerDay(String username, int days, String endDate, String startDate) {
        User user = userService.getByUsername(username);

        LocalDate rangeEnd = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        LocalDate rangeStart = startDate != null
                ? LocalDate.parse(startDate)
                : rangeEnd.minusDays(days - 1L);
        if (rangeEnd.isBefore(rangeStart)) {
            throw new IllegalArgumentException("Netinkamas intervalas: endDate yra prieš startDate");
        }

        // Fetch one extra day before the window to use as a baseline for each book
        LocalDateTime baseline = rangeStart.minusDays(1).atStartOfDay();

        List<ReadingProgress> logs = readingProgressRepository
                .findByUserIdAndUpdatedAtAfter(user.getId(), baseline);

        // Build output buckets (one per day in range, all starting at 0)
        Map<LocalDate, Integer> dailyPages = new LinkedHashMap<>();
        for (LocalDate d = rangeStart; !d.isAfter(rangeEnd); d = d.plusDays(1)) {
            dailyPages.put(d, 0);
        }

        logs.stream()
                .collect(Collectors.groupingBy(ReadingProgress::getBookId))
                .values()
                .forEach(bookLogs -> addPagesReadForBook(bookLogs, dailyPages, rangeStart, rangeEnd));

        return dailyPages.entrySet().stream()
                .map(e -> new PagesDayDataPoint(e.getKey().format(DAY_LABEL_FMT), e.getValue()))
                .toList();
    }

    /** Prideda vienos knygos per dieną perskaitytus puslapius prie dailyPages. */
    private void addPagesReadForBook(List<ReadingProgress> bookLogs, Map<LocalDate, Integer> dailyPages,
                                     LocalDate rangeStart, LocalDate rangeEnd) {
        // Max page reached on each date (may include baseline day)
        Map<LocalDate, Integer> maxByDate = new TreeMap<>();
        for (ReadingProgress log : bookLogs) {
            LocalDate logDate = log.getUpdatedAt().toLocalDate();
            if (!logDate.isAfter(rangeEnd)) {
                maxByDate.merge(logDate, log.getCurrentPage(), Math::max);
            }
        }

        // Running max before the window starts is our baseline
        int runningMax = maxByDate.entrySet().stream()
                .filter(e -> e.getKey().isBefore(rangeStart))
                .mapToInt(Map.Entry::getValue)
                .max()
                .orElse(0);

        for (LocalDate d = rangeStart; !d.isAfter(rangeEnd); d = d.plusDays(1)) {
            Integer dayMax = maxByDate.get(d);
            if (dayMax != null && dayMax > runningMax) {
                dailyPages.merge(d, dayMax - runningMax, Integer::sum);
                runningMax = dayMax;
            }
        }
    }

    public List<GenreDataPoint> getGenreStats(String username) {
        User user = userService.getByUsername(username);
        List<ReadingList> entries = readingListRepository.findAllByUserId(user.getId());

        return entries.stream()
                .map(e -> e.getBook().getGenre())
                .filter(genre -> genre != null && !genre.isBlank())
                .collect(Collectors.groupingBy(g -> g, Collectors.summingInt(g -> 1)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> new GenreDataPoint(e.getKey(), e.getValue()))
                .toList();
    }

    private StatsSummary buildSummary(List<ReadingList> entries) {
        Map<String, Long> byStatus = entries.stream()
                .collect(Collectors.groupingBy(ReadingList::getStatus, Collectors.counting()));
        return new StatsSummary(entries.size(),
                byStatus.getOrDefault("FINISHED", 0L).intValue(),
                byStatus.getOrDefault("READING", 0L).intValue(),
                byStatus.getOrDefault("WANT_TO_READ", 0L).intValue());
    }

    private LocalDateTime calculateSince(LocalDateTime now, String period) {
        return switch (period) {
            case DAY -> now.truncatedTo(ChronoUnit.DAYS);
            // Snap to Monday of anchor's ISO week so the window is always Mon–Sun(or today)
            case WEEK -> now.toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay();
            case MONTH -> now.minusDays(29).truncatedTo(ChronoUnit.DAYS);
            case YEAR -> now.minusMonths(11).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            default -> now.minusDays(6).truncatedTo(ChronoUnit.DAYS);
        };
    }

    private List<ReadingList> filterByRange(List<ReadingList> entries, LocalDateTime since, LocalDateTime anchor) {
        return entries.stream()
                .filter(e -> isWithin(e.getAddedAt(), since, anchor) || isWithin(e.getCompletedAt(), since, anchor))
                .toList();
    }

    private List<StatsDataPoint> buildDataPointsForRange(
            List<ReadingList> entries, LocalDateTime since, LocalDateTime anchor, String granularity) {
        LocalDate from = since.toLocalDate();
        LocalDate to = anchor.toLocalDate();
        Map<String, StatsDataPoint> buckets = switch (granularity) {
            case DAY -> dateBuckets(from, to, d -> d.plusDays(1), d -> d.format(DAY_LABEL_FMT));
            case WEEK -> dateBuckets(from.with(DayOfWeek.MONDAY), to.with(DayOfWeek.MONDAY),
                    d -> d.plusWeeks(1), this::weekLabel);
            case MONTH -> dateBuckets(from.withDayOfMonth(1), to.withDayOfMonth(1),
                    d -> d.plusMonths(1), d -> d.format(MONTH_LABEL_FMT));
            case YEAR -> dateBuckets(from.withDayOfYear(1), to.withDayOfYear(1),
                    d -> d.plusYears(1), d -> String.valueOf(d.getYear()));
            default -> throw new IllegalStateException("Nežinomas granuliarumas: " + granularity);
        };
        countEntries(entries, buckets, since, anchor, t -> toGranularityBucketKey(t, granularity));
        return new ArrayList<>(buckets.values());
    }

    private String toGranularityBucketKey(LocalDateTime dateTime, String granularity) {
        return switch (granularity) {
            case WEEK  -> weekLabel(dateTime.toLocalDate().with(DayOfWeek.MONDAY));
            case MONTH -> dateTime.toLocalDate().withDayOfMonth(1).format(MONTH_LABEL_FMT);
            case YEAR  -> String.valueOf(dateTime.getYear());
            default    -> dateTime.toLocalDate().format(DAY_LABEL_FMT);
        };
    }

    private String weekLabel(LocalDate monday) {
        return monday.format(DAY_LABEL_FMT);
    }

    private List<StatsDataPoint> buildDataPoints(List<ReadingList> entries, LocalDateTime now,
                                                 LocalDateTime since, String period) {
        LocalDate from = since.toLocalDate();
        LocalDate to = now.toLocalDate();
        Map<String, StatsDataPoint> buckets = switch (period) {
            case DAY -> hourBuckets();
            case YEAR -> dateBuckets(from.withDayOfMonth(1), to.withDayOfMonth(1),
                    d -> d.plusMonths(1), d -> d.format(MONTH_LABEL_FMT));
            default -> dateBuckets(from, to, d -> d.plusDays(1), d -> d.format(DAY_LABEL_FMT));
        };
        // Originalus kodas čia netikrino viršutinės ribos, todėl perduodam LocalDateTime.MAX
        countEntries(entries, buckets, since, LocalDateTime.MAX, t -> toBucketKey(t, period));
        return new ArrayList<>(buckets.values());
    }

    private String toBucketKey(LocalDateTime dateTime, String period) {
        return switch (period) {
            case DAY  -> String.format("%02d:00", dateTime.getHour());
            case YEAR -> dateTime.toLocalDate().withDayOfMonth(1).format(MONTH_LABEL_FMT);
            default   -> dateTime.toLocalDate().format(DAY_LABEL_FMT);
        };
    }

    private static Map<String, StatsDataPoint> hourBuckets() {
        Map<String, StatsDataPoint> buckets = new LinkedHashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            putEmpty(buckets, String.format("%02d:00", hour));
        }
        return buckets;
    }

    private static Map<String, StatsDataPoint> dateBuckets(LocalDate from, LocalDate to,
                                                           UnaryOperator<LocalDate> step,
                                                           Function<LocalDate, String> label) {
        Map<String, StatsDataPoint> buckets = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = step.apply(d)) {
            putEmpty(buckets, label.apply(d));
        }
        return buckets;
    }

    private static void putEmpty(Map<String, StatsDataPoint> buckets, String label) {
        buckets.put(label, new StatsDataPoint(label, 0, 0));
    }

    private static void countEntries(List<ReadingList> entries, Map<String, StatsDataPoint> buckets,
                                     LocalDateTime since, LocalDateTime until,
                                     Function<LocalDateTime, String> keyOf) {
        for (ReadingList entry : entries) {
            findBucket(buckets, entry.getAddedAt(), since, until, keyOf)
                    .ifPresent(p -> p.setBooksAdded(p.getBooksAdded() + 1));
            findBucket(buckets, entry.getCompletedAt(), since, until, keyOf)
                    .ifPresent(p -> p.setBooksCompleted(p.getBooksCompleted() + 1));
        }
    }

    private static Optional<StatsDataPoint> findBucket(Map<String, StatsDataPoint> buckets, LocalDateTime time,
                                                       LocalDateTime since, LocalDateTime until,
                                                       Function<LocalDateTime, String> keyOf) {
        if (!isWithin(time, since, until)) {
            return Optional.empty();
        }
        return Optional.ofNullable(buckets.get(keyOf.apply(time)));
    }

    private static boolean isWithin(LocalDateTime time, LocalDateTime since, LocalDateTime until) {
        return time != null && !time.isBefore(since) && !time.isAfter(until);
    }
}
