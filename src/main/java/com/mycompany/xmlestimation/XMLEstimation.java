package com.mycompany.xmlestimation;

import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

public class XMLEstimation {
    public static void main(String[] args){
        System.setProperty("entityExpansionLimit", "10000000");
        
        String XMLFileName = "C:/DBLP/dblp-2016-11-02.xml"; //Name of xml file parameter
        String csvFileName = "C:/Users/User/Desktop/dblpKmeans.csv"; //Name of csv file
        String resultFileName = "C:/Users/User/Desktop/dblpEstimation.txt"; //Result file name
        double ratio = 1.00; //Ratio parameter
        
        long N = 0;
        
        try{
            XMLStreamReader xmlr = XMLInputFactory.newInstance().createXMLStreamReader(XMLFileName, new FileInputStream(XMLFileName));
            
            int nBegins = 0;
            ArrayList<java.lang.Integer> startTags = new ArrayList();
            
            BitSet isLeaf = new BitSet();
            
            while (xmlr.hasNext()){
                xmlr.next();
                
                if (xmlr.isStartElement()){
                    nBegins++;
                    startTags.add(nBegins);
                }
                
                if (xmlr.isCharacters() && xmlr.getText().trim().length()>0){
                    isLeaf.set(startTags.get(startTags.size()-1));
                }
                
                if (xmlr.isEndElement()){
                    startTags.remove(startTags.size()-1);
                }  
            }
            
            Map<String, java.lang.Integer> howManyMustBe = new HashMap();
            nBegins = 0;
            int k;
            int inside_lvl;
            int lvl = 0;
            
            xmlr = XMLInputFactory.newInstance().createXMLStreamReader(XMLFileName, new FileInputStream(XMLFileName));
            
            while (xmlr.hasNext()){
                xmlr.next();
                
                if (xmlr.isStartElement()){
                    nBegins++;
                    if (!isLeaf.get(nBegins)){
                        if (lvl!=0){
                            if (!howManyMustBe.keySet().contains(xmlr.getLocalName()))
                                howManyMustBe.put(xmlr.getLocalName(), 1);
                            else {
                                k = howManyMustBe.get(xmlr.getLocalName());
                                howManyMustBe.put(xmlr.getLocalName(), k+1);
                            }
                        }
                    }
                    else {
                        inside_lvl = 1;
                        do{
                            xmlr.next();
                            if (xmlr.isStartElement()){
                                inside_lvl++;
                                nBegins++;
                            }
                            if (xmlr.isEndElement())
                                inside_lvl--;
                        }
                        while(!(xmlr.isEndElement() && inside_lvl==0));
                    }
                    lvl++;
                }
                
                if (xmlr.isEndElement())
                    lvl--;
            }
            
            for (String str: howManyMustBe.keySet()){
                k = howManyMustBe.get(str);
                howManyMustBe.put(str, (int)(k*ratio));
            }
            
            ArrayList<String> cluster_names = new ArrayList();
            cluster_names.addAll(howManyMustBe.keySet());
            
            Map<String,java.lang.Integer> howManyThereAre = new HashMap();
            
            for (String str: howManyMustBe.keySet())
                howManyThereAre.put(str, 0);
            
            int n_clusters = howManyMustBe.keySet().size();
            
            //Exclude mastersthesis
            howManyMustBe.put("mastersthesis", 0);
            n_clusters--;
            cluster_names.remove("mastersthesis");
            //
            
            long[][] table = new long[n_clusters][n_clusters];
            for (int i = 0; i<n_clusters; i++)
                for (int j = 0; j<n_clusters; j++)
                    table[i][j] = 0;
            
            lvl = 0;
            nBegins = 0;
            startTags.clear();
            
            BufferedReader br = new BufferedReader(new FileReader(csvFileName));
            
            xmlr = XMLInputFactory.newInstance().createXMLStreamReader(XMLFileName, new FileInputStream(XMLFileName));
            
            while (xmlr.hasNext()){
                xmlr.next();
                
                if (xmlr.isStartElement()){
                    nBegins++;
                    startTags.add(nBegins);
                    if (isLeaf.get(nBegins)){
                        inside_lvl = 1;
                        do{
                            xmlr.next();
                            if (xmlr.isStartElement()){
                                inside_lvl++;
                                nBegins++;
                            }
                            if (xmlr.isEndElement())
                                inside_lvl--;
                        }
                        while(!(xmlr.isEndElement() && inside_lvl==0));
                    }
                    lvl++;
                }
                
                if (xmlr.isEndElement()){
                    if (!isLeaf.get(startTags.get(startTags.size()-1)) && lvl!=1)
                        if (howManyThereAre.get(xmlr.getLocalName()) < howManyMustBe.get(xmlr.getLocalName())){
                        k = howManyThereAre.get(xmlr.getLocalName());
                        howManyThereAre.put(xmlr.getLocalName(), k+1);
                        table[Integer.parseInt(br.readLine())-1][cluster_names.indexOf(xmlr.getLocalName())]++;
                        N++;
                        System.out.println(N);
                    }
                    startTags.remove(startTags.size()-1);
                    lvl--;
                }
            }
            
            
            
            
            long[] cluster = new long[n_clusters];
            long[] klass = new long[n_clusters];
        
            for (int i=0;i<n_clusters;i++){
                cluster[i] = 0;
                klass[i] = 0;
            }
        
            for (int j=0;j<n_clusters;j++)
                for (int i=0;i<n_clusters;i++)
                    cluster[j]+=table[i][j];
        
            for (int i=0;i<n_clusters;i++)
                for (int j=0;j<n_clusters;j++)
                    klass[i]+=table[i][j];
        
            long SS = 0;
            long SD = 0;
            long DS = 0;
            long DD = 0;
        
            long buf;
        
            for (int i=0;i<n_clusters;i++)
                for (int j=0;j<n_clusters;j++){
                    buf = table[i][j];
                    SS += buf *(buf - 1)/2;
                    SD += buf *(cluster[j]-buf);
                    DS += buf *(klass[i]-buf);
                }
        
            DD = N*(N-1)/2 - SS - SD - DS;
        
            double Rand;
            double Jaccard;
            double FM;
        
            Rand = (double)(SS + DD)/(double)(SS + DS + SD + DD);
            Jaccard = (double)SS / (double)(SS + SD + DS);
            FM = SS / Math.sqrt((double)(SS + SD) * (double)(SS + DS));
            
            FileWriter fw = new FileWriter(resultFileName,false);
            
            for (int i=0;i<n_clusters;i++)
                fw.append(cluster_names.get(i) + "  ");
            
            fw.append("\n");
            
            for (int i=0;i<n_clusters;i++){
                for (int j=0;j<n_clusters;j++)
                    fw.append(table[i][j] + "  ");
                fw.append("\n");
            }
            
            fw.append("\n");
            
            fw.append("Rand statistic = "+Rand+"\n");
            fw.append("Jaccard index = "+Jaccard+"\n"); 
            fw.append("Folkes and Mallows index = "+FM+"\n");
//            fw.append("F1-measure = "+F1+"\n");
            fw.close();
            
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
}
