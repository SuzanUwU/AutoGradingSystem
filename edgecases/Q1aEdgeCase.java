/* 
    static method getIsogramWords take in a ArrayList<String>,
    only returns the words where characters are unique
    
    cannot test for numbers since exam sample says assume the list only contain alphabets
    cannot test for empty ArrayList or null Array
    cannot test for null words

    test for:
    - single character words
    - same character in upper, lower (aA)
    - 2 identical characters
    - eg. abACBc 
    - empty strings
    - mix of valid/invalid
    - duplicate words
    
*/

import java.util.*;

public class Q1aEdgeCase extends Q1a {
    public static void test(ArrayList<String> input, ArrayList<String> expected) {
        ArrayList<String> actual = getIsogramWords(input);
        System.out.println("Input   : " + input);
        System.out.println("Expected: " + expected);
        System.out.println("Actual  : " + actual);
        
        if (expected.equals(actual)) {
            System.out.println("PASSED");
        } else {
            System.out.println("FAILED");
        }

        System.out.println("================================================");

    }
    public static void main(String[] args) {
        // single character
        test(new ArrayList<>(Arrays.asList("a")), new ArrayList<>(Arrays.asList("a")));
        // same character in upper and lower
        test(new ArrayList<>(Arrays.asList("aA")), new ArrayList<>());
        // identical characters
        test(new ArrayList<>(Arrays.asList("aa")), new ArrayList<>());
        // mixed valid / invalid words
        test(new ArrayList<>(Arrays.asList("abc", "aA", "hello", "WorLd", "xyz")), new ArrayList<>(Arrays.asList("abc", "WorLd", "xyz")));
        test(new ArrayList<>(Arrays.asList("Apple", "banana", "DOG", "AbCd", "aabb")), new ArrayList<>(Arrays.asList("DOG", "AbCd")));
        test(new ArrayList<>(Arrays.asList("AbCd", "xYz", "Hello", "aBcDe")), new ArrayList<>(Arrays.asList("AbCd", "xYz", "aBcDe")));
        test(new ArrayList<>(Arrays.asList("abc", "abc", "aA", "xyz")), new ArrayList<>(Arrays.asList("abc", "abc", "xyz")));
    }
}
