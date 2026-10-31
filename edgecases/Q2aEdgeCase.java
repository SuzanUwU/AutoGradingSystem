/* 
    static method getAverageAge takes in String filename and String surname
    return average age of ppl in the file where surname matches (double)
    surname in input file can only be either first/last word and is in uppercase
    
    if file don't exist -1.0
    if file has no person w that surname 0.0
    - every person has a surname
    - no need to round the value

    test for:
    - nonexistent file
    - no one with the surname
    - one matching / odd number matching
    - surname at end / beginning of name
    - different length names
    - eg. ONG does not get WONG YONG etc.
*/

import java.io.*;
import java.util.concurrent.Flow.Subscriber;

public class Q2aEdgeCase extends Q2a{
    
    public static void test(String filename, String surname, double expected) {
            double actual = getAverageAge(filename, surname);
            System.out.println("getAverageAge: " + filename + ", " + surname);
            System.out.println("Expected : " + expected);
            System.out.println("Actual   : " + actual);

            if (expected == actual) {
                System.out.println("PASSED");
            } else {
                System.out.println("FAILED");
            }
            System.out.println("================================================");
    }

    public static void main(String[] args) {
        test("Edge.txt", "ONG", -1.0);
        test("personsEdgeCase.txt", "SMITH", 20.5);
        test("personsEdgeCase.txt", "ZERO", 0.0);
        test("personsEdgeCase.txt", "LEE", 30.5);
        test("personsEdgeCase.txt", "TAN", 55.0);
        test("personsEdgeCase.txt", "ONG", 18.0);
        test("personsEdgeCase.txt", "WONG", 30.5);
    }
}
