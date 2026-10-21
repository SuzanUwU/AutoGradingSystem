/*
 * Name: Ping Lee
 * Email ID: ping.lee.2023@computing.smu.edu.sg
 */

import java.util.*;

public class Q1a {

    /*
     * Write the method getIsogramWords
     */
    public static ArrayList<String> getIsogramWords(ArrayList<String> inputs) {
        // insert your code here.
        ArrayList<String> ans = new ArrayList<>();
        for(int i=0;i<inputs.size();i++){
            String word = inputs.get(i);
            ArrayList<String> check = new ArrayList<>();
            boolean add = true;
            for(int u=0;u<word.length();u++){
                if(check.contains((String.valueOf(word.charAt(u))).toLowerCase())){
                    add = false;
                }
                check.add((String.valueOf(word.charAt(u))).toLowerCase());
            }
            if(add){
                ans.add(word);
            }
        }
        return ans; // to make this code compile. Please modify accordingly!
    }


    // Do not modify the following codes
    public static void main(String[] args) {
        int tcNum = 1;
        {
            ArrayList<String> inputs = new ArrayList<>();
            inputs = new ArrayList<>();
            inputs.add("cat");
            inputs.add("dog");
            inputs.add("tiger");

            System.out.println("-------------------------------------------------------");
            System.out.printf("Test %d: getIsogramWords(%s)%n", tcNum++, inputs);
            String expected = "[cat, dog, tiger]";
            ArrayList<String> actual = getIsogramWords(inputs);
            System.out.println("Expected : " + expected);
            System.out.println("Actual   : " + actual);
            if (expected.equals(actual.toString())) {
                System.out.println("Passed");
            } else {
                System.out.println("Failed");
            }
            System.out.println("-------------------------------------------------------");
        }

        {
            ArrayList<String> inputs = new ArrayList<>();
            inputs = new ArrayList<>();
            inputs.add("alan");
            inputs.add("ben");
            inputs.add("cindy");
            inputs.add("danny");
            inputs.add("evan");
            
            System.out.println("-------------------------------------------------------");
            System.out.printf("Test %d: getIsogramWords(%s)%n", tcNum++, inputs);
            String expected = "[ben, cindy, evan]";
            ArrayList<String> actual = getIsogramWords(inputs);
            System.out.println("Expected : " + expected);
            System.out.println("Actual   : " + actual);
            if (expected.equals(actual.toString())) {
                System.out.println("Passed");
            } else {
                System.out.println("Failed");
            }
            System.out.println("-------------------------------------------------------");
        }

        {
            ArrayList<String> inputs = new ArrayList<>();
            inputs = new ArrayList<>();
            inputs.add("Alan");
            inputs.add("Ben");
            inputs.add("Cindy");
            inputs.add("Danny");
            inputs.add("Evelyne");
            
            System.out.println("-------------------------------------------------------");
            System.out.printf("Test %d: getIsogramWords(%s)%n", tcNum++, inputs);
            String expected = "[Ben, Cindy]";
            ArrayList<String> actual = getIsogramWords(inputs);
            System.out.println("Expected : " + expected);
            System.out.println("Actual   : " + actual);
            if (expected.equals(actual.toString())) {
                System.out.println("Passed");
            } else {
                System.out.println("Failed");
            }
            System.out.println("-------------------------------------------------------");
        }
    }
}