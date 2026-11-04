/* 
    has Q3.java, ShapeComparator.java
    static method sortShapes take in list<Shape>
    returns list of shapes with area <= 1000 and sorted by area in descending and perimeter in descending
    must complete code in ShapeComparator

    cannot test for null or empty ArrayList
    test for:
    - area 0
    - 1000 / 1001
    - equal area / equal perimeter
    - same area / diff perimeter
    - input order opp to expected sorting order
    - only 1 shape filtered / all shapes filtered
*/

import java.util.*;

public class Q3EdgeCase extends Q3 {
    public static void test(List<Shape> input, String expected) {
        List<Shape> actual = sortShapes(input);

        System.out.println("Input     : " + input);
        System.out.println("Expected  : " + expected);
        System.out.println("Actual    : " + actual);

        if (expected.equals(actual.toString())) {
            System.out.println("PASSED");
        } else {
            System.out.println("FAILED");
        }
        System.out.println("=========================================");
    }

    public static void main(String[] args) {
        // area 0
        ArrayList<Shape> shapes1 = new ArrayList<>();
        shapes1.add(new Rectangle("R1", 0, 10));
        shapes1.add(new Rectangle("R2", 5, 6));
        test(shapes1, "[R2=>30.0:22.0, R1=>0.0:20.0]");

        // area exactly 1000
        ArrayList<Shape> shapes2 = new ArrayList<>();
        shapes2.add(new Rectangle("R1", 100, 10));
        shapes2.add(new Rectangle("R2", 50, 10));
        test(shapes2, "[R1=>1000.0:220.0, R2=>500.0:120.0]");

        // area over by 1: 1001
        ArrayList<Shape> shapes3 = new ArrayList<>();
        shapes3.add(new Rectangle("R1", 100.1, 10));
        shapes3.add(new Rectangle("R2", 50, 10));
        test(shapes3, "[R2=>500.0:120.0]");

        // 1000 and 1001 together
        ArrayList<Shape> shapes4 = new ArrayList<>();
        shapes4.add(new Rectangle("R1", 100.1, 10)); // 1001
        shapes4.add(new Rectangle("R2", 100, 10)); // 1000
        shapes4.add(new Rectangle("R3", 50, 10)); // 500
        test(shapes4, "[R2=>1000.0:220.0, R3=>500.0:120.0]");

        // all filtered out
        ArrayList<Shape> shapes5 = new ArrayList<>();
        shapes5.add(new Rectangle("R1", 101, 10));
        shapes5.add(new Circle("C1", 20));
        shapes5.add(new Rectangle("R2", 120, 10));
        test(shapes5, "[]");

        // same area, diff perimeter
        ArrayList<Shape> shapes6 = new ArrayList<>();
        shapes6.add(new Rectangle("R1", 10, 20));
        shapes6.add(new Rectangle("R2", 5, 40));
        test(shapes6, "[R2=>200.0:90.0, R1=>200.0:60.0]");

        ArrayList<Shape> shapes7 = new ArrayList<>();
        shapes7.add(new Rectangle("R1", 10, 20));
        shapes7.add(new Rectangle("R2", 5, 40));
        shapes7.add(new Rectangle("R3", 8, 25));
        test(shapes7, "[R2=>200.0:90.0, R3=>200.0:66.0, R1=>200.0:60.0]");

        // same area, same perimeter (keep input order)
        ArrayList<Shape> shapes8 = new ArrayList<>();
        shapes8.add(new Rectangle("R1", 10, 20));
        shapes8.add(new Rectangle("R2", 20, 10));
        test(shapes8, "[R1=>200.0:60.0, R2=>200.0:60.0]");

        // 2 shapes same area & same perimeter, 1 diff
        ArrayList<Shape> shapes9 = new ArrayList<>();
        shapes9.add(new Rectangle("R1", 10, 20));
        shapes9.add(new Rectangle("R2", 20, 10));
        shapes9.add(new Rectangle("R3", 25, 8));
        test(shapes9, "[R3=>200.0:66.0, R1=>200.0:60.0, R2=>200.0:60.0]");

        // mixed circles and rectangles
        ArrayList<Shape> shapes10 = new ArrayList<>();
        shapes10.add(new Rectangle("R1", 20, 10));
        shapes10.add(new Circle("C1", 10));
        shapes10.add(new Circle("C2", 5));
        test(shapes10, "[C1=>314.0:63.0, R1=>200.0:60.0, C2=>79.0:31.0]");

        ArrayList<Shape> shapes11 = new ArrayList<>();
        shapes11.add(new Circle("C1", 5));
        shapes11.add(new Rectangle("R1", 79, 1));
        test(shapes11, "[R1=>79.0:160.0, C1=>79.0:31.0]");

        // expected sorting opp of input order
        ArrayList<Shape> shapes12 = new ArrayList<>();
        shapes12.add(new Rectangle("R1", 2, 2));
        shapes12.add(new Rectangle("R2", 5, 10));
        shapes12.add(new Rectangle("R3", 10, 10));
        shapes12.add(new Rectangle("R4", 20, 10));
        test(shapes12, "[R4=>200.0:60.0, R3=>100.0:40.0, R2=>50.0:30.0, R1=>4.0:8.0]");

        // little of everything
        ArrayList<Shape> shapes13 = new ArrayList<>();
        shapes13.add(new Circle("C2", 20));
        shapes13.add(new Rectangle("R2", 5, 40));
        shapes13.add(new Circle("C1", 10));
        shapes13.add(new Rectangle("R1", 100, 10));
        shapes13.add(new Rectangle("R3", 10, 20));
        test(shapes13, "[R1=>1000.0:220.0, C1=>314.0:63.0, R2=>200.0:90.0, R3=>200.0:60.0]");

        // 1 shape only (filtered & not filtered)
        ArrayList<Shape> shapes14 = new ArrayList<>();
        shapes14.add(new Circle("C1", 5));
        test(shapes14, "[C1=>79.0:31.0]");
        
        ArrayList<Shape> shapes15 = new ArrayList<>();
        shapes15.add(new Circle("C1", 20));
        test(shapes15, "[]");

        // duplicate shapes
        ArrayList<Shape> shapes16 = new ArrayList<>();
        shapes16.add(new Rectangle("R1", 10, 20));
        shapes16.add(new Rectangle("R2", 10, 20));
        shapes16.add(new Circle("C1", 5));
        test(shapes16, "[R1=>200.0:60.0, R2=>200.0:60.0, C1=>79.0:31.0]");
    }
}
