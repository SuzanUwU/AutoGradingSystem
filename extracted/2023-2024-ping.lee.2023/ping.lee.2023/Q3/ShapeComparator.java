/*
 * Name: Ping Lee
 * Email ID: ping.lee.2023@computing.smu.edu.sg
 */

import java.util.*;   

public class ShapeComparator implements Comparator<Shape>{

    public int compare(Shape o1, Shape o2) {
        if(o1.getArea() != o2.getArea()){
            if((o2.getArea() - o1.getArea())>0){
                return 1;
            }
            else{
                return -1;
            }
        }
        if(o1.getPerimeter() != o2.getPerimeter()){
            if((o2.getPerimeter() - o1.getPerimeter())>0){
                return 1;
            }
            else{
                return -1;
            }
        }
        return 0;
    }

}
