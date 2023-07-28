package searchengine.lemma;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.config.LemmaConfiguration;
import searchengine.exception.CurrentRuntimeException;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@Slf4j
public record LemmaEngine(LemmaConfiguration lemmaConfiguration) {


    public Map<String, Integer> getLemmaMap(String text) {
        text = arrayContainsWords(text);
        String[] elements = text.toLowerCase(Locale.ROOT).split("\\s+");
        return Arrays.stream(elements)
                .flatMap(el -> {
                    try {
                        return getLemma(el).stream();
                    } catch (Exception e) {
                        throw new CurrentRuntimeException(e.getMessage());
                    }
                })
                .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(e -> 1)));
    }

    public List<String> getLemma(String word) throws IOException {
        List<String> lemmaList = new ArrayList<>();
        if (checkLanguage(word).equals("Russian")) {
            List<String> baseRusForm = lemmaConfiguration.russianLuceneMorphology().getNormalForms(word);
            if (!word.isEmpty() && !isCorrectWordForm(word)) {
                lemmaList.addAll(baseRusForm);
            }
        }

        return lemmaList;
    }

    private boolean isCorrectWordForm(String word) throws IOException {
        List<String> morphForm = lemmaConfiguration.russianLuceneMorphology().getMorphInfo(word);
        for (String l : morphForm) {
            if (l.contains("ПРЕДЛ") || l.contains("СОЮЗ") || l.contains("МЕЖД") || l.contains("ВВОДН") || l.contains("ЧАСТ") || l.length() <= 3) {
                return true;
            }
        }
        return false;
    }


    private String checkLanguage(String word) {
        String russianAlphabet = "[а-яА-Я]+";
        String englishAlphabet = "[a-zA-Z]+";

        if (word.matches(russianAlphabet)) {
            return "Russian";
        } else if (word.matches(englishAlphabet)) {
            return "English";
        } else {
            return "";
        }
    }

    private String arrayContainsWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ").trim();
    }

    public Collection<Integer> findLemmaIndexInText(String content, String lemma) throws IOException {
        String[] elements = content.toLowerCase(Locale.ROOT)
                .split("\\p{Punct}|\\s");
        List<String> allLemmas = new ArrayList<>();
        for (String el : elements) {
            allLemmas.addAll(getLemma(el));
        }
        return IntStream.range(0, allLemmas.size())
                .filter(i -> allLemmas.get(i).equals(lemma))
                .mapToObj(Integer::valueOf)
                .collect(Collectors.toList());
    }
}