package com.unicodesearch.index;

import com.unicodesearch.model.Document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads the multilingual corpus from disk (corpus/<language>/*.txt) into
 * Document objects. Files are always read as UTF-8 to avoid corrupting
 * Indic-script characters, and the Document class derives its NFC-normalized
 * and case-folded searchable forms so every downstream algorithm operates on
 * consistent text while the original (with its line breaks) is kept for display.
 */
public class CorpusLoader {

    public List<Document> loadCorpus(Path corpusRoot) throws IOException {
        List<Document> documents = new ArrayList<>();
        if (!Files.isDirectory(corpusRoot)) {
            throw new IOException("Corpus directory not found: " + corpusRoot.toAbsolutePath());
        }

        try (Stream<Path> languageDirs = Files.list(corpusRoot)) {
            for (Path langDir : languageDirs.filter(Files::isDirectory).sorted().toList()) {
                String language = langDir.getFileName().toString();
                try (Stream<Path> files = Files.list(langDir)) {
                    for (Path file : files.filter(p -> p.toString().endsWith(".txt")).sorted().toList()) {
                        String fileRaw = Files.readString(file, StandardCharsets.UTF_8);
                        String id = language + "-" + file.getFileName().toString().replace(".txt", "");

                        // Documents may optionally start with a "TITLE: <title>" line
                        // (used by the generated 100-article-per-language corpus) so
                        // each article has a real, unique, human-readable title rather
                        // than a generic "Document N" label. Older hand-written sample
                        // documents without this line keep working exactly as before.
                        String title;
                        String raw;
                        if (fileRaw.startsWith("TITLE:")) {
                            int newline = fileRaw.indexOf('\n');
                            title = fileRaw.substring("TITLE:".length(), newline < 0 ? fileRaw.length() : newline).trim();
                            raw = (newline < 0) ? "" : fileRaw.substring(newline + 1).stripLeading();
                        } else {
                            title = deriveTitle(language, file.getFileName().toString());
                            raw = fileRaw;
                        }

                        // The Document derives its own NFC-normalized and case-folded
                        // (searchable) forms; line breaks in the original are preserved.
                        documents.add(new Document(id, title, language, raw));
                    }
                }
            }
        }
        return documents;
    }

    private String deriveTitle(String language, String filename) {
        String base = filename.replace(".txt", "").replace("document", "Document ");
        String cap = language.substring(0, 1).toUpperCase() + language.substring(1);
        return cap + " " + base.trim();
    }
}
