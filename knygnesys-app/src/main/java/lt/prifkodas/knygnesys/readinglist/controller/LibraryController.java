package lt.prifkodas.knygnesys.readinglist.controller;

import lombok.RequiredArgsConstructor;
import lt.prifkodas.knygnesys.readinglist.dto.AddBookRequest;
import lt.prifkodas.knygnesys.readinglist.dto.LibraryBookDto;
import lt.prifkodas.knygnesys.readinglist.dto.UpdateProgressRequest;
import lt.prifkodas.knygnesys.readinglist.service.LibraryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class LibraryController implements LibraryApi {

    private final LibraryService libraryService;

    @Override
    public ResponseEntity<List<LibraryBookDto>> getLibrary(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(libraryService.getLibrary(userDetails.getUsername()));
    }

    @Override
    public ResponseEntity<List<LibraryBookDto>> filterLibrary(
            @AuthenticationPrincipal UserDetails userDetails,
            String title,
            String author,
            Integer progress) {
        return ResponseEntity.ok(libraryService.filterLibrary(
                userDetails.getUsername(),
                title,
                author,
                progress
        ));
    }

    @Override
    public ResponseEntity<LibraryBookDto> addBook(@AuthenticationPrincipal UserDetails userDetails, AddBookRequest request) {
        LibraryBookDto dto = libraryService.addBook(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Override
    public ResponseEntity<Void> uploadEpub(@AuthenticationPrincipal UserDetails userDetails, Integer id, MultipartFile file) throws IOException {
        libraryService.uploadEpub(userDetails.getUsername(), id, file);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<byte[]> getEpub(@AuthenticationPrincipal UserDetails userDetails, Integer id) throws IOException {
        byte[] epubBytes = libraryService.getEpub(userDetails.getUsername(), id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/epub+zip"));
        headers.setContentDispositionFormData("inline", "book.epub");
        return ResponseEntity.ok().headers(headers).body(epubBytes);
    }

    @Override
    public ResponseEntity<Void> updateProgress(@AuthenticationPrincipal UserDetails userDetails, Integer id, UpdateProgressRequest request) {
        libraryService.updateProgress(userDetails.getUsername(), id, request.getCurrentPage());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> removeBook(@AuthenticationPrincipal UserDetails userDetails, Integer id) throws IOException {
        libraryService.removeBook(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
