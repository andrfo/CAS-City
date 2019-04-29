package utils;

import java.util.*;

public class Kruskal {
    public static class EDGE implements Comparable<EDGE>{
        public char from;
		public char to;
        int weight;
        public EDGE(char f, char t, int w){
            from = f;
            to = t;
            weight = w; 
        }  

        @Override
        public int compareTo(EDGE o) {
            return weight<o.weight?-1:(weight>o.weight?1:0);
        } 
        
        @Override
        public String toString(){
            return "["+from+", "+to+"]";
        }
    }
    
    private static Map<Character, Character> PARENT;
    private static Map<Character, Integer> RANKS; //to store the depths
    
    public static void initialize(char[] universe){ 
        PARENT = new HashMap<>();
        RANKS = new HashMap<>();
        for(char x:universe){
            PARENT.put(x, x);
            RANKS.put(x, 1);
        } 
    }
    
    public static char FindSet(char item){
        char parent = PARENT.get(item); 
        if(parent==item)return item;
        else return FindSet(parent);
    }
    
    public static void Union(char setA, char setB){
        char pA, pB;
        while((pA = PARENT.get(setA))!=setA){setA = pA;}
        while((pB = PARENT.get(setB))!=setB){setB = pB;}
        
        int rankFirst = RANKS.get(setA), rankSecond = RANKS.get(setB);
        if(rankFirst>rankSecond){
            PARENT.put(setB, setA);  
            updateRanksUpward(setB);
        }else if(rankSecond>rankFirst){
            PARENT.put(setA, setB);  
            updateRanksUpward(setA);
        }else{
            PARENT.put(setB, setA); 
            updateRanksUpward(setB);
        }
    }
    
    public static void updateRanksUpward(char current){
        int currentDepth = RANKS.get(current);
        char currentsParent = PARENT.get(current);
        int parentsDepth = RANKS.get(currentsParent);
        if(!(currentDepth<parentsDepth || currentsParent == current)){ 
            RANKS.put(currentsParent, currentDepth+1);
            updateRanksUpward(currentsParent);
        }
    } 
    
    public static void main(String[] args){
        //Test data
        //CLRS Example p632
        char[] vertices = new char[]{'a','b','c','d','e','f','g','h','i'}; 
        EDGE[] edges = new EDGE[14];
        edges[0] = new EDGE('a','b',4);
        edges[1] = new EDGE('a','h',8);
        edges[2] = new EDGE('b','h',11);
        edges[3] = new EDGE('b','c',8);
        edges[4] = new EDGE('h','g',1);
        edges[5] = new EDGE('h','i',7);
        edges[6] = new EDGE('c','i',2);
        edges[7] = new EDGE('i','g',6);
        edges[8] = new EDGE('g','f',2);
        edges[9] = new EDGE('f','c',4);
        edges[10] = new EDGE('c','d',7);
        edges[11] = new EDGE('f','d',14);
        edges[12] = new EDGE('f','e',10);
        edges[13] = new EDGE('d','e',9);
        
        //Call Kruskal Algorithm
        kruskal(vertices, edges);
    }
    
    //CLSR p631 Algorithm
    public static ArrayList<EDGE> kruskal(char[] vertices, EDGE[] edges){  
            //Initialize A = empty set
            ArrayList<EDGE> mst = new ArrayList<>();
            
            //for each vertex v belongs to G.V MAKE-SET(v)
            initialize(vertices);
             
            //sort the edges of G.E into non decreasing order by weight w
            Arrays.sort(edges);
            
            //For each edge (u,v) belongs to G.E taken in non decreasing order by weight
            for(EDGE edge:edges){
                //If (find-set(u)!=find-set(v)
                if(FindSet(edge.from)!=FindSet(edge.to)){
                    //A = A union (u, v)
                    mst.add(edge);
                    //UNION(u, v)
                    Union(edge.from, edge.to);
                }
            }             
            //Display contents
//            System.out.println("MST contains the edges: "+mst);
            return mst;
    }
}
