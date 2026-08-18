package com.azhost.service;

import com.azhost.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SlugService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern MULTI_DASH = Pattern.compile("[-]+");

    private final ProjectRepository projectRepository;

    public SlugService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "project";
        }

        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = MULTI_DASH.matcher(slug).replaceAll("-");
        slug = slug.toLowerCase(Locale.ENGLISH);

        // Strip leading and trailing dashes
        if (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        if (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }

        return slug.isBlank() ? "project" : slug;
    }

    public String generateUniqueSlug(UUID userId, String name) {
        String baseSlug = toSlug(name);
        String candidateSlug = baseSlug;
        int counter = 2;

        while (projectRepository.existsByUserIdAndSlug(userId, candidateSlug)) {
            candidateSlug = baseSlug + "-" + counter;
            counter++;
        }

        return candidateSlug;
    }
}
