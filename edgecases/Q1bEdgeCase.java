/* 
    static method getSumOfEvenIntegers that take in ArrayList<Object>
    returns int: sum of all even INTEGERS in array
    - "2", '2' shld not be counted

    cannot test for null or empty ArrayList
    test for:
    - 0
    - negative integers
    - string numbers
    - null
    - boolean
    - char 
    - float / double
    - extreme int values
*/

import java.util.*;

public class Q1bEdgeCase extends Q1b{
    public static void test(ArrayList<Object> input, int expected) {
        int actual = getSumOfEvenIntegers(input);

        System.out.println("Input   : " + input);
        System.out.println("Expected: " + expected);
        System.out.println("Actual  : " + actual);

        if (expected == actual) {
            System.out.println("PASSED");
        } else {
            System.out.println("FAILED");
        }

        System.out.println("================================================");
    }

    public static void main(String[] args) {
        test(new ArrayList<Object>(Arrays.asList(0)), 0);
        test(new ArrayList<Object>(Arrays.asList(-2, -4, -6)), -12);
        test(new ArrayList<Object>(Arrays.asList(-3, -5, -7)), 0);
        test(new ArrayList<Object>(Arrays.asList(-3, -2, -5, 6)), 4);
        test(new ArrayList<Object>(Arrays.asList(true, false, 4, "word", 'B')), 4);
        test(new ArrayList<Object>(Arrays.asList(true, "4", '6', 1)), 0);
        test(new ArrayList<Object>(Arrays.asList(2, "2", 4.0, 3, '8', false, -6)), -4);
        test(new ArrayList<Object>(Arrays.asList(-10, 3, 8, "12", -4, 7)), -6);
        
        Object obj = new Object();
        test(new ArrayList<Object>(Arrays.asList(2, obj, 4, -6)), 0);
    }
    
}
