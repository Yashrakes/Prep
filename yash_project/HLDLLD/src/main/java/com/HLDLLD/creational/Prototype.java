package com.HLDLLD.creational;

import java.util.HashMap;

abstract  class Docu implements Cloneable{
    private String title;
    private String content;

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // Abstract method for displaying the document
    abstract void display();

    // Clone method
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
class Report extends Docu {
    public Report() {
        setTitle("Default Report");
        setContent("This is a default report.");
    }

    @Override
    void display() {
        System.out.println("Report: " + getTitle());
        System.out.println("Content: " + getContent());
    }
}

class Invoice extends Docu {
    public Invoice() {
        setTitle("Default Invoice");
        setContent("This is a default invoice.");
    }

    @Override
    void display() {
        System.out.println("Invoice: " + getTitle());
        System.out.println("Content: " + getContent());
    }
}

class DocumentCache {
    private static HashMap<String, Docu> documentMap = new HashMap<>();

    // Load the cache with default documents
    public static void loadCache() {
        Report report = new Report();
        documentMap.put("Report", report);

        Invoice invoice = new Invoice();
        documentMap.put("Invoice", invoice);
    }

    // Retrieve a cloned document by type
    public static Docu getDocument(String type) {
        Docu cachedDocument = documentMap.get(type);
        return (Docu) cachedDocument.clone();
    }
}


public class Prototype {
    public static void main(String[] args) {
        // Load the document cache
        DocumentCache.loadCache();

        // Retrieve and customize a Report document
        Docu report = DocumentCache.getDocument("Report");
        report.setTitle("Sales Report");
        report.setContent("This is the sales report for Q1 2024.");
        report.display();

        // Retrieve and customize an Invoice document
        Docu invoice = DocumentCache.getDocument("Invoice");
        invoice.setTitle("Client Invoice");
        invoice.setContent("Invoice for client XYZ, amount $5000.");
        invoice.display();

        // Clone another Report document without customization
        Docu clonedReport = DocumentCache.getDocument("Report");
        clonedReport.display();
    }
}
