package lt.prifkodas.knygnesys.readinglist.service;

import lombok.RequiredArgsConstructor;
import lt.prifkodas.knygnesys.book.Book;
import lt.prifkodas.knygnesys.book.repository.BookRepository;
import lt.prifkodas.knygnesys.progress.ReadingProgress;
import lt.prifkodas.knygnesys.progress.repository.ReadingProgressRepository;
import lt.prifkodas.knygnesys.readinglist.ReadingList;
import lt.prifkodas.knygnesys.readinglist.dto.AddBookRequest;
import lt.prifkodas.knygnesys.readinglist.dto.LibraryBookDto;
import lt.prifkodas.knygnesys.readinglist.repository.ReadingListRepository;
import lt.prifkodas.knygnesys.shared.exception.ResourceNotFoundException;
import lt.prifkodas.knygnesys.user.User;
import lt.prifkodas.knygnesys.user.service.UserService;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private final ReadingListRepository readingListRepository;
    private final BookRepository bookRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final UserService userService;

    @Value("${knygnesys.epub.storage-dir:./epub-storage}")
    private String storageDir;

    public LibraryBookDto addBook(String username, AddBookRequest request) {
        User user = userService.getByUsername(username);

        Book book = bookRepository.findByOlId(request.getOlId()).orElseGet(() -> {
            Book newBook = new Book();
            newBook.setOlId(request.getOlId());
            newBook.setTitle(request.getTitle());
            newBook.setAuthor(request.getAuthor());
            newBook.setCoverUrl(request.getCoverUrl());
            newBook.setPageCount(request.getPageCount());
            newBook.setGenre(request.getGenre());
            return bookRepository.save(newBook);
        });

        readingListRepository.findByUserIdAndBookId(user.getId(), book.getId()).ifPresent(existing -> {
            throw new IllegalArgumentException("Knyga jau yra jūsų bibliotekoje");
        });

        ReadingList entry = new ReadingList();
        entry.setUser(user);
        entry.setBook(book);
        entry.setStatus(request.getStatus() != null ? request.getStatus() : "WANT_TO_READ");

        return toDto(readingListRepository.save(entry));
    }

    public List<LibraryBookDto> getLibrary(String username) {
        User user = userService.getByUsername(username);
        return readingListRepository.findAllByUserId(user.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<LibraryBookDto> filterLibrary(String username, String title, String author, Integer progress) {
        User user = userService.getByUsername(username);
        return readingListRepository.findByUserIdWithFilters(
                        user.getId(),
                        title != null ? title.toLowerCase() : null,
                        author != null ? author.toLowerCase() : null,
                        progress != null && progress >= 0 ? progress : null
                )
                .stream()
                .map(this::toDto)
                .toList();
    }

    public void uploadEpub(String username, Integer readingListId, MultipartFile file) throws IOException {
        User user = userService.getByUsername(username);
        ReadingList entry = readingListRepository.findByIdAndUserId(readingListId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bibliotekos įrašas nerastas"));

        Path storePath = Paths.get(storageDir);
        Files.createDirectories(storePath);

        String filename = UUID.randomUUID() + ".epub";
        Path filePath = storePath.resolve(filename);

        if (entry.getEpubPath() != null) {
            Files.deleteIfExists(Paths.get(entry.getEpubPath()));
        }

        Files.write(filePath, file.getBytes());
        entry.setEpubPath(filePath.toString());
        readingListRepository.save(entry);
    }

    public byte[] getEpub(String username, Integer readingListId) throws IOException {
        User user = userService.getByUsername(username);
        ReadingList entry = readingListRepository.findByIdAndUserId(readingListId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bibliotekos įrašas nerastas"));

        if (entry.getEpubPath() == null) {
            throw new ResourceNotFoundException("EPUB failas nerastas");
        }

        return Files.readAllBytes(Paths.get(entry.getEpubPath()));
    }

    public void removeBook(String username, Integer readingListId) throws IOException {
        User user = userService.getByUsername(username);
        ReadingList entry = readingListRepository.findByIdAndUserId(readingListId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bibliotekos įrašas nerastas"));

        if (entry.getEpubPath() != null) {
            Files.deleteIfExists(Paths.get(entry.getEpubPath()));
        }

        readingListRepository.delete(entry);
    }

    public void updateProgress(String username, Integer readingListId, int currentPage) {
        User user = userService.getByUsername(username);
        ReadingList entry = readingListRepository.findByIdAndUserId(readingListId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bibliotekos įrašas nerastas"));
        entry.setCurrentPage(currentPage);
        Integer pageCount = entry.getBook().getPageCount();
        if (pageCount != null && currentPage >= pageCount) {
            entry.setStatus("FINISHED");
            entry.setCompletedAt(LocalDateTime.now());
        } else if ("WANT_TO_READ".equals(entry.getStatus()) && currentPage > 0) {
            entry.setStatus("READING");
        }
        readingListRepository.save(entry);
        readingProgressRepository.save(new ReadingProgress(user.getId(), entry.getBook().getId(), currentPage));
    }

    private LibraryBookDto toDto(ReadingList entry) {
        return new LibraryBookDto(
                entry.getId(),
                entry.getBook().getId(),
                entry.getBook().getOlId(),
                entry.getBook().getTitle(),
                entry.getBook().getAuthor(),
                entry.getBook().getCoverUrl(),
                entry.getBook().getPageCount(),
                entry.getCurrentPage() != null ? entry.getCurrentPage() : 0,
                entry.getStatus(),
                entry.getAddedAt() != null ? entry.getAddedAt().toString() : null,
                entry.getEpubPath() != null
        );
    }
}
