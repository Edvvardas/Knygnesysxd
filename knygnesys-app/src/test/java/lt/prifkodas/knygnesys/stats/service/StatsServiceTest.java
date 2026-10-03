package lt.prifkodas.knygnesys.stats.service;

import lt.prifkodas.knygnesys.book.Book;
import lt.prifkodas.knygnesys.progress.ReadingProgress;
import lt.prifkodas.knygnesys.progress.repository.ReadingProgressRepository;
import lt.prifkodas.knygnesys.readinglist.ReadingList;
import lt.prifkodas.knygnesys.readinglist.repository.ReadingListRepository;
import lt.prifkodas.knygnesys.stats.dto.GenreDataPoint;
import lt.prifkodas.knygnesys.stats.dto.PagesDayDataPoint;
import lt.prifkodas.knygnesys.stats.dto.StatsResponse;
import lt.prifkodas.knygnesys.user.User;
import lt.prifkodas.knygnesys.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private ReadingListRepository readingListRepository;

    @Mock
    private ReadingProgressRepository readingProgressRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private StatsService statsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setUsername("testuser");
    }

    // getStats() tests

    @Test
    void getStats_invalidPeriod_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> statsService.getStats("testuser", "INVALID", null, null, null));
    }

    @Test
    void getStats_weekPeriod_returnsSummaryAndDataPoints() {
        // Use a known past Sunday so the Mon-snap always gives a full 7-day window.
        // 2025-04-06 is Sunday; its ISO week is Mon 2025-03-31 – Sun 2025-04-06.
        String sunday = "2025-04-06";
        ReadingList entry = new ReadingList();
        entry.setStatus("READING");
        entry.setAddedAt(LocalDateTime.of(2025, 4, 3, 10, 0)); // Thursday of that week

        when(userService.getByUsername("testuser")).thenReturn(user);
        when(readingListRepository.findAllByUserId(1)).thenReturn(List.of(entry));

        StatsResponse response = statsService.getStats("testuser", "WEEK", sunday, null, null);

        assertEquals("WEEK", response.getPeriod());
        assertEquals(1, response.getSummary().getTotalBooks());
        assertEquals(1, response.getSummary().getReading());
        assertEquals(0, response.getSummary().getCompleted());
        assertEquals(7, response.getDataPoints().size());
    }

    @Test
    void getStats_yearPeriod_returnsMonthlyBuckets() {
        when(userService.getByUsername("testuser")).thenReturn(user);
        when(readingListRepository.findAllByUserId(1)).thenReturn(List.of());

        StatsResponse response = statsService.getStats("testuser", "YEAR", null, null, null);

        assertEquals("YEAR", response.getPeriod());
        assertEquals(12, response.getDataPoints().size());
    }

    @Test
    void getStats_monthPeriod_returnsDailyBuckets() {
        when(userService.getByUsername("testuser")).thenReturn(user);
        when(readingListRepository.findAllByUserId(1)).thenReturn(List.of());

        StatsResponse response = statsService.getStats("testuser", "MONTH", null, null, null);

        assertEquals("MONTH", response.getPeriod());
        assertEquals(30, response.getDataPoints().size());
    }

    @Test
    void getStats_finishedBook_countedInCompleted() {
        ReadingList entry = new ReadingList();
        entry.setStatus("FINISHED");
        entry.setAddedAt(LocalDateTime.now());
        entry.setCompletedAt(LocalDateTime.now());

        when(userService.getByUsername("testuser")).thenReturn(user);
        when(readingListRepository.findAllByUserId(1)).thenReturn(List.of(entry));

        StatsResponse response = statsService.getStats("testuser", "WEEK", null, null, null);

        assertEquals(1, response.getSummary().getCompleted());
        assertEquals(0, response.getSummary().getReading());
    }

    @Test
    void getStats_customRangeWithWeekGranularity_usesIsoWeekBuckets() {
        ReadingList entry = new ReadingList();
        entry.setStatus("READING");
        entry.setAddedAt(LocalDateTime.of(2025, 4, 10, 9, 0));

        when(userService.getByUsername("testuser")).thenReturn(user);
        when(readingListRepository.findAllByUserId(1)).thenReturn(List.of(entry));

        StatsResponse response = statsService.getStats(
                "testuser",
                "WEEK",
                null,
                "2025-04-01",
                "2025-04-30"
        );

        assertEquals("WEEK", response.getPeriod());
        assertFalse(response.getDataPoints().isEmpty());
        // Labels are formatted as "d MMM" in Lithuanian (e.g. "7 bal.") — the Monday of each ISO week.
        assertTrue(response.getDataPoints().stream().anyMatch(p -> p.getLabel().matches("\\d{1,2} [a-ząčęėįšųūž]+\\.?")));
        assertEquals(1, response.getDataPoints().stream().mapToInt(p -> p.getBooksAdded()).sum());
    }

    @Test
    void getStats_customRange_endBeforeStart_throws() {
        when(userService.getByUsername("testuser")).thenReturn(user);
        when(readingListRepository.findAllByUserId(1)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () ->
                statsService.getStats("testuser", "DAY", null, "2025-04-10", "2025-04-01"));
    }

    // getPagesPerDay() tests

    @Test
    void getPagesPerDay_noProgress_returnsZeroFilledBuckets() {
        when(userService.getByUsername("testuser")).thenReturn(user);
        when(readingProgressRepository.findByUserIdAndUpdatedAtAfter(eq(1), any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<PagesDayDataPoint> result = statsService.getPagesPerDay("testuser", 7, null, null);

        assertEquals(7, result.size());
        assertTrue(result.stream().allMatch(p -> p.getPagesRead() == 0));
    }

    @Test
    void getPagesPerDay_withProgress_returnsCorrectPageCounts() {
        ReadingProgress progress = new ReadingProgress(1, 1, 100);
        progress.setUpdatedAt(LocalDateTime.now());

        when(userService.getByUsername("testuser")).thenReturn(user);
        when(readingProgressRepository.findByUserIdAndUpdatedAtAfter(eq(1), any(LocalDateTime.class)))
                .thenReturn(List.of(progress));

        List<PagesDayDataPoint> result = statsService.getPagesPerDay("testuser", 7, null, null);

        assertEquals(7, result.size());
        int totalPages = result.stream().mapToInt(PagesDayDataPoint::getPagesRead).sum();
        assertEquals(100, totalPages);
    }

    @Test
    void getPagesPerDay_endDate_usesProvidedWindow() {
        when(userService.getByUsername("testuser")).thenReturn(user);
        when(readingProgressRepository.findByUserIdAndUpdatedAtAfter(eq(1), any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<PagesDayDataPoint> result = statsService.getPagesPerDay("testuser", 7, "2025-04-06", null);

        assertEquals(7, result.size());
        assertEquals("31 kov.", result.getFirst().getDate());
        assertEquals("6 bal.", result.getLast().getDate());
        assertTrue(result.stream().allMatch(p -> p.getPagesRead() == 0));
    }

    @Test
    void getPagesPerDay_customStartEnd_usesExactRange() {
        when(userService.getByUsername("testuser")).thenReturn(user);
        when(readingProgressRepository.findByUserIdAndUpdatedAtAfter(eq(1), any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<PagesDayDataPoint> result = statsService.getPagesPerDay("testuser", 30, "2025-04-10", "2025-04-03");

        assertEquals(8, result.size());
        assertEquals("3 bal.", result.getFirst().getDate());
        assertEquals("10 bal.", result.getLast().getDate());
    }

    @Test
    void getPagesPerDay_customStartEnd_endBeforeStart_throws() {
        when(userService.getByUsername("testuser")).thenReturn(user);

        assertThrows(IllegalArgumentException.class, () ->
                statsService.getPagesPerDay("testuser", 30, "2025-04-01", "2025-04-10"));
    }

    // getGenreStats() tests

    @Test
    void getGenreStats_noBooks_returnsEmptyList() {
        when(userService.getByUsername("testuser")).thenReturn(user);
        when(readingListRepository.findAllByUserId(1)).thenReturn(List.of());

        List<GenreDataPoint> result = statsService.getGenreStats("testuser");

        assertTrue(result.isEmpty());
    }

    @Test
    void getGenreStats_withGenres_returnsSortedByCountDesc() {
        Book fantasy1 = new Book();
        fantasy1.setGenre("Fantasy");
        Book fantasy2 = new Book();
        fantasy2.setGenre("Fantasy");
        Book scifi = new Book();
        scifi.setGenre("SciFi");

        ReadingList e1 = new ReadingList();
        e1.setBook(fantasy1);
        e1.setStatus("FINISHED");
        e1.setAddedAt(LocalDateTime.now());
        ReadingList e2 = new ReadingList();
        e2.setBook(fantasy2);
        e2.setStatus("FINISHED");
        e2.setAddedAt(LocalDateTime.now());
        ReadingList e3 = new ReadingList();
        e3.setBook(scifi);
        e3.setStatus("READING");
        e3.setAddedAt(LocalDateTime.now());

        when(userService.getByUsername("testuser")).thenReturn(user);
        when(readingListRepository.findAllByUserId(1)).thenReturn(List.of(e1, e2, e3));

        List<GenreDataPoint> result = statsService.getGenreStats("testuser");

        assertEquals(2, result.size());
        assertEquals("Fantasy", result.get(0).getGenre());
        assertEquals(2, result.get(0).getCount());
        assertEquals("SciFi", result.get(1).getGenre());
        assertEquals(1, result.get(1).getCount());
    }

    @Test
    void getGenreStats_bookWithNullGenre_excluded() {
        Book noGenre = new Book();
        noGenre.setGenre(null);
        Book withGenre = new Book();
        withGenre.setGenre("Drama");

        ReadingList e1 = new ReadingList();
        e1.setBook(noGenre);
        e1.setStatus("READING");
        e1.setAddedAt(LocalDateTime.now());
        ReadingList e2 = new ReadingList();
        e2.setBook(withGenre);
        e2.setStatus("READING");
        e2.setAddedAt(LocalDateTime.now());

        when(userService.getByUsername("testuser")).thenReturn(user);
        when(readingListRepository.findAllByUserId(1)).thenReturn(List.of(e1, e2));

        List<GenreDataPoint> result = statsService.getGenreStats("testuser");

        assertEquals(1, result.size());
        assertEquals("Drama", result.get(0).getGenre());
    }
}
