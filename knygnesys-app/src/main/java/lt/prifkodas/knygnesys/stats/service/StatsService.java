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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final java.util.Locale LOCALE_LT = new java.util.Locale("lt", "LT");
    private static final DateTimeFormatter DAY_LABEL_FMT   = DateTimeFormatter.ofPattern("d MMM", LOCALE_LT);
    private static final DateTimeFormatter MONTH_LABEL_FMT = DateTimeFormatter.ofPattern("MMM yyyy", LOCALE_LT);

    private final ReadingListRepository readingListRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final UserService userService;

    public StatsResponse getStats(String username, String period, String date, String startDate, String endDate) {
        String normalizedPeriod = period.toUpperCase();
        if (!List.of("DAY", "WEEK", "MONTH", "YEAR").contains(normalizedPeriod)) {
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
                : rangeEnd.minusDays(days - 1);
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

        // Group logs by bookId, then compute pages-read-per-day per book
        Map<Integer, List<ReadingProgress>> byBook = logs.stream()
                .collect(Collectors.groupingBy(ReadingProgress::getBookId));

        for (List<ReadingProgress> bookLogs : byBook.values()) {
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

        return dailyPages.entrySet().stream()
                .map(e -> new PagesDayDataPoint(e.getKey().format(DAY_LABEL_FMT), e.getValue()))
                .toList();
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
        int total = entries.size();
        int completed = 0;
        int reading = 0;
        int wantToRead = 0;

        for (ReadingList entry : entries) {
            switch (entry.getStatus()) {
                case "FINISHED" -> completed++;
                case "READING" -> reading++;
                case "WANT_TO_READ" -> wantToRead++;
            }
        }

        return new StatsSummary(total, completed, reading, wantToRead);
    }

    private LocalDateTime calculateSince(LocalDateTime now, String period) {
        return switch (period) {
            case "DAY" -> now.truncatedTo(ChronoUnit.DAYS);
            // Snap to Monday of anchor's ISO week so the window is always Mon–Sun(or today)
            case "WEEK" -> now.toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay();
            case "MONTH" -> now.minusDays(29).truncatedTo(ChronoUnit.DAYS);
            case "YEAR" -> now.minusMonths(11).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            default -> now.minusDays(6).truncatedTo(ChronoUnit.DAYS);
        };
    }

    private List<ReadingList> filterByRange(List<ReadingList> entries, LocalDateTime since, LocalDateTime anchor) {
        return entries.stream()
                .filter(e -> (e.getAddedAt() != null && !e.getAddedAt().isBefore(since) && !e.getAddedAt().isAfter(anchor))
                        || (e.getCompletedAt() != null && !e.getCompletedAt().isBefore(since) && !e.getCompletedAt().isAfter(anchor)))
                .toList();
    }

    private List<StatsDataPoint> buildDataPointsForRange(
            List<ReadingList> entries, LocalDateTime since, LocalDateTime anchor, String granularity) {
        Map<String, StatsDataPoint> buckets = new LinkedHashMap<>();

        switch (granularity) {
            case "DAY" -> {
                LocalDate cursor = since.toLocalDate();
                while (!cursor.isAfter(anchor.toLocalDate())) {
                    String label = cursor.format(DAY_LABEL_FMT);
                    buckets.put(label, new StatsDataPoint(label, 0, 0));
                    cursor = cursor.plusDays(1);
                }
            }
            case "WEEK" -> {
                LocalDate startWeek = since.toLocalDate().with(DayOfWeek.MONDAY);
                LocalDate endWeek = anchor.toLocalDate().with(DayOfWeek.MONDAY);
                LocalDate cursor = startWeek;
                while (!cursor.isAfter(endWeek)) {
                    String label = weekLabel(cursor);
                    buckets.put(label, new StatsDataPoint(label, 0, 0));
                    cursor = cursor.plusWeeks(1);
                }
            }
            case "MONTH" -> {
                LocalDate cursor = since.toLocalDate().withDayOfMonth(1);
                LocalDate endMonth = anchor.toLocalDate().withDayOfMonth(1);
                while (!cursor.isAfter(endMonth)) {
                    String label = cursor.format(MONTH_LABEL_FMT);
                    buckets.put(label, new StatsDataPoint(label, 0, 0));
                    cursor = cursor.plusMonths(1);
                }
            }
            case "YEAR" -> {
                LocalDate cursor = since.toLocalDate().withDayOfYear(1);
                LocalDate endYear = anchor.toLocalDate().withDayOfYear(1);
                while (!cursor.isAfter(endYear)) {
                    String label = String.valueOf(cursor.getYear());
                    buckets.put(label, new StatsDataPoint(label, 0, 0));
                    cursor = cursor.plusYears(1);
                }
            }
        }

        for (ReadingList entry : entries) {
            if (entry.getAddedAt() != null && !entry.getAddedAt().isBefore(since) && !entry.getAddedAt().isAfter(anchor)) {
                String key = toGranularityBucketKey(entry.getAddedAt(), granularity);
                StatsDataPoint point = buckets.get(key);
                if (point != null) {
                    point.setBooksAdded(point.getBooksAdded() + 1);
                }
            }

            if (entry.getCompletedAt() != null && !entry.getCompletedAt().isBefore(since) && !entry.getCompletedAt().isAfter(anchor)) {
                String key = toGranularityBucketKey(entry.getCompletedAt(), granularity);
                StatsDataPoint point = buckets.get(key);
                if (point != null) {
                    point.setBooksCompleted(point.getBooksCompleted() + 1);
                }
            }
        }

        return new ArrayList<>(buckets.values());
    }

    private String toGranularityBucketKey(LocalDateTime dateTime, String granularity) {
        return switch (granularity) {
            case "DAY"   -> dateTime.toLocalDate().format(DAY_LABEL_FMT);
            case "WEEK"  -> weekLabel(dateTime.toLocalDate().with(DayOfWeek.MONDAY));
            case "MONTH" -> dateTime.toLocalDate().withDayOfMonth(1).format(MONTH_LABEL_FMT);
            case "YEAR"  -> String.valueOf(dateTime.getYear());
            default      -> dateTime.toLocalDate().format(DAY_LABEL_FMT);
        };
    }

    private String weekLabel(LocalDate monday) {
        return monday.format(DAY_LABEL_FMT);
    }

    private List<StatsDataPoint> buildDataPoints(List<ReadingList> entries, LocalDateTime now,
                                                  LocalDateTime since, String period) {
        Map<String, StatsDataPoint> buckets = new LinkedHashMap<>();

        if ("DAY".equals(period)) {
            for (int hour = 0; hour < 24; hour++) {
                String label = String.format("%02d:00", hour);
                buckets.put(label, new StatsDataPoint(label, 0, 0));
            }
        } else if ("YEAR".equals(period)) {
            LocalDate start = since.toLocalDate().withDayOfMonth(1);
            LocalDate end = now.toLocalDate().withDayOfMonth(1);
            while (!start.isAfter(end)) {
                String label = start.format(MONTH_LABEL_FMT);
                buckets.put(label, new StatsDataPoint(label, 0, 0));
                start = start.plusMonths(1);
            }
        } else {
            LocalDate start = since.toLocalDate();
            LocalDate end = now.toLocalDate();
            while (!start.isAfter(end)) {
                String label = start.format(DAY_LABEL_FMT);
                buckets.put(label, new StatsDataPoint(label, 0, 0));
                start = start.plusDays(1);
            }
        }

        for (ReadingList entry : entries) {
            if (entry.getAddedAt() != null && !entry.getAddedAt().isBefore(since)) {
                String key = toBucketKey(entry.getAddedAt(), period);
                StatsDataPoint point = buckets.get(key);
                if (point != null) {
                    point.setBooksAdded(point.getBooksAdded() + 1);
                }
            }

            if (entry.getCompletedAt() != null && !entry.getCompletedAt().isBefore(since)) {
                String key = toBucketKey(entry.getCompletedAt(), period);
                StatsDataPoint point = buckets.get(key);
                if (point != null) {
                    point.setBooksCompleted(point.getBooksCompleted() + 1);
                }
            }
        }

        return new ArrayList<>(buckets.values());
    }

    private String toBucketKey(LocalDateTime dateTime, String period) {
        return switch (period) {
            case "DAY"  -> String.format("%02d:00", dateTime.getHour());
            case "YEAR" -> dateTime.toLocalDate().withDayOfMonth(1).format(MONTH_LABEL_FMT);
            default     -> dateTime.toLocalDate().format(DAY_LABEL_FMT);
        };
    }
}
