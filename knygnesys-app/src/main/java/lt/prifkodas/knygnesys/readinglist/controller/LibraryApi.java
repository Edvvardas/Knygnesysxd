package lt.prifkodas.knygnesys.readinglist.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lt.prifkodas.knygnesys.readinglist.dto.AddBookRequest;
import lt.prifkodas.knygnesys.readinglist.dto.LibraryBookDto;
import lt.prifkodas.knygnesys.readinglist.dto.UpdateProgressRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "Biblioteka", description = "Vartotojo knygų biblioteka")
@RequestMapping("/api/library")
public interface LibraryApi {

    @Operation(summary = "Gauti vartotojo biblioteką")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Biblioteka grąžinta"),
            @ApiResponse(responseCode = "401", description = "Neprisijungęs")
    })
    @GetMapping
    ResponseEntity<List<LibraryBookDto>> getLibrary(@AuthenticationPrincipal UserDetails userDetails);

    @Operation(summary = "Pridėti knygą į biblioteką")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Knyga pridėta"),
            @ApiResponse(responseCode = "400", description = "Knyga jau bibliotekoje"),
            @ApiResponse(responseCode = "401", description = "Neprisijungęs")
    })
    @PostMapping
    ResponseEntity<LibraryBookDto> addBook(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AddBookRequest request
    );

    @Operation(summary = "Įkelti EPUB failą")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "EPUB įkeltas"),
            @ApiResponse(responseCode = "404", description = "Įrašas nerastas"),
            @ApiResponse(responseCode = "401", description = "Neprisijungęs")
    })
    @PostMapping("/{id}/epub")
    ResponseEntity<Void> uploadEpub(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file
    ) throws IOException;

    @Operation(summary = "Gauti EPUB failą")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "EPUB failas"),
            @ApiResponse(responseCode = "404", description = "EPUB nerastas"),
            @ApiResponse(responseCode = "401", description = "Neprisijungęs")
    })
    @GetMapping("/{id}/epub")
    ResponseEntity<byte[]> getEpub(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Integer id
    ) throws IOException;

    @Operation(summary = "Atnaujinti skaitymo progresą")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Progresas atnaujintas"),
            @ApiResponse(responseCode = "404", description = "Įrašas nerastas"),
            @ApiResponse(responseCode = "401", description = "Neprisijungęs")
    })
    @PutMapping("/{id}/progress")
    ResponseEntity<Void> updateProgress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Integer id,
            @RequestBody UpdateProgressRequest request
    );

    @Operation(summary = "Filtruoti biblioteką pagal pavadinimą, autorių arba progresą")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Filtruota biblioteka grąžinta"),
            @ApiResponse(responseCode = "401", description = "Neprisijungęs")
    })
    @GetMapping("/filter")
    ResponseEntity<List<LibraryBookDto>> filterLibrary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer progress
    );

    @Operation(summary = "Pašalinti knygą iš bibliotekos")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pašalinta"),
            @ApiResponse(responseCode = "404", description = "Įrašas nerastas"),
            @ApiResponse(responseCode = "401", description = "Neprisijungęs")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> removeBook(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Integer id
    ) throws IOException;
}
