/* 
    static method getTopStudent take in String filename, String coursename
    format: John LEE,IS101#4.0-IS102#3.0-IS103#2.5
    returns name and gpa of student w highest GPA of that course (John LEE-4.0)
    returns first occurence if multiple students w same highest gpa
    throws DataException if file don't exist or no course w that name

    test for:
    - no file / no course
    - tie in GPA
    - first occurrence wins
    - course only appear once
    - course appear at diff positions
*/

import java.io.*;

public class Q2bEdgeCase extends Q2b {
    public static void test(String filename, String course, String expected) {
        try {
            String actual = getTopStudent(filename, course);
            System.out.println("getTopStudent(" + filename + ", " + course + ")");
            System.out.println("Expected : " + expected);
            System.out.println("Actual   : " + actual);

            if (expected.equals(actual)) {
                System.out.println("PASSED");
            } else {
                System.out.println("FAILED");
            }
        } catch (Exception e) {
            System.out.println("FAILED, EXCEPTION");
            e.printStackTrace();
        }
        System.out.println("================================================");
    }

    public static void testException(String filename, String course) {
        try {
            getTopStudent(filename, course);
            System.out.println("FAILED, DATAEXCEPTION");
        } catch (DataException e) {
            System.out.println("PASSED, DATAEXCEPTION");
        } catch (Exception e) {
            System.out.println("FAILED, WRONG EXCEPTION: " + e.getClass().getName());
        }
        System.out.println("================================================");

    }

    public static void main(String[] args) {
        testException("studentsEdgeCase.txt", "CS999");
        testException("unknown.txt", "CS101");
        test("studentsEdgeCase.txt", "CS101", "Alice TAN-4.0");
        test("studentsEdgeCase.txt", "CS102", "Charlie ONG-4.0");
        test("studentsEdgeCase.txt", "CS103", "Ger LIM-3.8");
        test("studentsEdgeCase.txt", "CS104", "Grace KOH-3.0");

    }
}
