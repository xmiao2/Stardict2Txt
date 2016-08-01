/*
 * A simple tool converting stardict database format to SQLite.
 * Copyright (C) 2015, Nguyễn Anh Tuấn
 * Email: anhtuanbk57@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

import db.StardictManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class StardictToTxt {

    public static final Map<String, String> wordPinyinOverride = new HashMap<String, String>(){{
        put("䦆头", "jué tóu");
        put("令人注目", "lìng rén zhù mù");
        put("建安七子", "jiàn ān qī zǐ");
    }};
    public static final String HETERONYM_PINYIN_MATCHER = "\\/.*?(?: |$|\\n|·)";
    public static final String PINYIN_VALIDATOR = "[ a-zāēīōūǖáéíóúǘǎěǐǒǔǚàèìòùǜü]+";
    public static final String UNICODE_LETTER_G = "ɡ";
    public static final String CELEBRITY_YEAR_MATCHER = "(?: |)(?:\\(|（).*?(?:\\)|）)";
    public static final String WHITESPACE_MATCHER = "[\\s| |　]+";
    public static final String UNICODE_CHAR_GUI = "龟";
    public static final String UNICODE_PUNCUATION = "[!（）ㄍ'“”《》{}…\\-\\?]+";
    public static final String WORD_SEPARATOR_MATCHER = "，|,|﹐|·|、|—|;";

    public static Map<String, Integer> charToFrequencyMap = new HashMap<>();
    public static Map<String, ElementEntry> wordToElementMap = new HashMap<>();
    public static Map<String, List<Pinyin>> charToPinyinsMap = new HashMap<>();

    public static class Pinyin implements Comparable<Pinyin> {
        private String pinyin;
        private int frequency;

        public int getFrequency() {
            return frequency;
        }

        public Pinyin(String pinyin) {
            this.pinyin = pinyin;
            frequency = 0;
        }

        public String getPinyin() {
            return pinyin;
        }

        public void incFrequency() {
            frequency++;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj == null) {
                return false;
            }
            if(!Pinyin.class.isAssignableFrom(obj.getClass())) {
                return false;
            }
            final Pinyin other = (Pinyin) obj;
            return other.pinyin.equals(this.pinyin);
        }

        @Override
        public int hashCode() {
            return this.pinyin.hashCode();
        }

        @Override
        public int compareTo(Pinyin o) {
            return Integer.compare(this.frequency, o.frequency);
        }
    }

    public static class ElementEntry {
        private String word;
        private String pinyin;
        private String frequency;
        private int pinyinFrequency;

        public String getWord() {
            return word;
        }

        public String getPinyin() {
            return pinyin;
        }

        public String getFrequency() {
            return frequency;
        }

        public int getPinyinFrequency() {
            return pinyinFrequency;
        }

        public ElementEntry(String word, String pinyin) {
            this(word, pinyin, 0);
        }

        public ElementEntry(String word, String pinyin, int pinyinFrequency) {
            this.word = word;
            this.pinyin = pinyin;
            this.frequency = calcFrequency(word);
            this.pinyinFrequency = pinyinFrequency;
        }

        private void setPinyinFrequency(final int pinyinFrequency) {
            this.pinyinFrequency = pinyinFrequency;
        }

        private String calcFrequency(final String word) {
            List<String> list = new ArrayList();
            word.codePoints().forEach(codepoint -> {
                String element = new String(Character.toChars(codepoint));
                Integer frequency = (frequency = charToFrequencyMap.get(element)) == null ? 0: frequency;
                list.add(Integer.toString(frequency));
            });
            return String.join(",", list);
        }
    }

    public static void main(String[] args) {
        String dictName = "hycihai";


        try {
            Files.lines(Paths.get("data/" + dictName + ".dict")).forEach(line -> {
                line.codePoints().forEach(codepoint -> {
                    String element = new String(Character.toChars(codepoint));
                    Integer oldVal = charToFrequencyMap.putIfAbsent(element, new Integer(0));
                    if(oldVal != null) {
                        charToFrequencyMap.put(element, new Integer(oldVal+1));
                    }
                });
            });
            int a = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dictName + ".txt"), StandardCharsets.UTF_8))) {
            StardictManager manager = new StardictManager();
            manager.setDictFilesLocation("data/", dictName);

            while (manager.nextWordAvailable()) {
                StardictManager.StardictWord stardictWord = manager.nextStardictWord();
                String word = stardictWord.getWord()
                        .replaceAll(CELEBRITY_YEAR_MATCHER, "")
                        .replaceAll(UNICODE_PUNCUATION, "")
                        .replaceAll(WHITESPACE_MATCHER, "");
                String originalPinyin = stardictWord.getDefinition().split("\n")[0];
                String pinyin = originalPinyin
                        .toLowerCase()
                        .replaceAll("gu3", " gǔ ")
                        .replaceAll("金屚", " lòu ")
                        .replaceAll("宬", " chéng ")
                        .replaceAll("䁖", " lōu ")
                        .replaceAll("璁", "cōng")
                        .replaceAll("〇|○", " líng ")
                        .replaceAll(UNICODE_CHAR_GUI, " guī ")
                        .replaceAll(UNICODE_LETTER_G, "g")
                        .replaceAll(CELEBRITY_YEAR_MATCHER, "")
                        .replaceAll(UNICODE_PUNCUATION, " ")
                        .replaceAll(HETERONYM_PINYIN_MATCHER, "")
                        .replaceAll(WHITESPACE_MATCHER, " ")
                        .trim();

                pinyin = PinyinSeparator.separate(pinyin)
                        .replaceAll(WHITESPACE_MATCHER, " ");

                pinyin = wordPinyinOverride.getOrDefault(word, pinyin);

                String[] words = word.split(WORD_SEPARATOR_MATCHER);
                String[] pinyins = pinyin.split(WORD_SEPARATOR_MATCHER);
                if(words.length != pinyins.length) {
                    continue;
                }

                for(int i=0; i<words.length; i++) {
                    String curWord = words[i].trim();
                    String curPinyin = pinyins[i].trim();
                    if(!curPinyin.matches(PINYIN_VALIDATOR)) {
                        continue;
                    }

                    if(curWord.length() != curPinyin.split(" ").length) {
                        continue;
                    }

                    wordToElementMap.putIfAbsent(word, new ElementEntry(curWord, curPinyin));
                }
            }

            wordToElementMap.values().stream().forEach(elementEntry -> {
                String[] chars = elementEntry.getWord().split("");
                String[] pinyins = elementEntry.getPinyin().split(" ");
                for(int i=0; i<chars.length; i++) {
                    Pinyin pinyin = new Pinyin(pinyins[i]);
                    List<Pinyin> newValue = new ArrayList<>();
                    List<Pinyin> oldValue = charToPinyinsMap.putIfAbsent(chars[i], newValue);
                    int index = -1;
                    if (oldValue == null) {
                        newValue.add(pinyin);
                    } else if ((index = oldValue.indexOf(pinyin)) >= 0) {
                        oldValue.get(index).incFrequency();
                    } else {
                        oldValue.add(pinyin);
                    }
                }
            });

            charToPinyinsMap.entrySet().stream().forEach(entry -> {
                String character = entry.getKey();
                Pinyin pinyin = Collections.max(entry.getValue());
                String pinyinString = pinyin.getPinyin();
                int pinyinFrequency = pinyin.getFrequency();
                wordToElementMap.put(character, new ElementEntry(character, pinyinString, pinyinFrequency));
            });

            for(ElementEntry elementEntry : wordToElementMap.values()) {
                String output = String.format("%s|%s|%s|%s%n", elementEntry.getWord(),
                        elementEntry.getPinyin(), elementEntry.getFrequency(),
                        elementEntry.getPinyinFrequency());
                out.write(output);
            }

            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
