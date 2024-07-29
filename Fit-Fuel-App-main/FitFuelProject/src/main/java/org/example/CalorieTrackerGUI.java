package org.example;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

public class CalorieTrackerGUI extends JFrame {
    private DefaultCategoryDataset dataset;
    private DefaultPieDataset pieDataset;
    private JTextField foodInput;
    private JTextField calorieInput;
    private JTextField dailyLimitInput;
    private JTextArea logTextArea;
    private JLabel calorieCountLabel;
    private JLabel goalLabel;
    private JComboBox<String> dayComboBox;
    private int dailyCalorieLimit = 2000;
    private List<List<Integer>> calorieLogs;
    private List<Map<String, Integer>> foodLogs;
    private MongoCollection<Document> collection;

    public CalorieTrackerGUI() {
        setTitle("Calorie Tracker");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        calorieLogs = new ArrayList<>();
        foodLogs = new ArrayList<>();
        calorieLogs.add(new ArrayList<>());
        foodLogs.add(new HashMap<>());

        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase database = mongoClient.getDatabase("calorie_tracker");
        collection = database.getCollection("calorie_logs");

        JPanel controlsPanel = new JPanel();
        JLabel limitLabel = new JLabel("Set Daily Calorie Limit:");
        dailyLimitInput = new JTextField(5);
        dailyLimitInput.setText(Integer.toString(dailyCalorieLimit));
        JButton setLimitButton = new JButton("Set Limit");
        setLimitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setDailyCalorieLimit();
            }
        });
        JLabel foodLabel = new JLabel("Enter Food:");
        foodInput = new JTextField(10);
        JLabel inputLabel = new JLabel("Enter Calorie Intake:");
        calorieInput = new JTextField(10);
        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addCalorieEntry();
            }
        });
        JButton viewLogButton = new JButton("View Log");
        viewLogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewCalorieLog();
            }
        });

        controlsPanel.add(limitLabel);
        controlsPanel.add(dailyLimitInput);
        controlsPanel.add(setLimitButton);
        controlsPanel.add(foodLabel);
        controlsPanel.add(foodInput);
        controlsPanel.add(inputLabel);
        controlsPanel.add(calorieInput);
        controlsPanel.add(addButton);
        controlsPanel.add(viewLogButton);
        getContentPane().add(controlsPanel, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new GridLayout(1, 2));
        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel rightPanel = new JPanel(new BorderLayout());

        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        getContentPane().add(logScrollPane, BorderLayout.CENTER);

        calorieCountLabel = new JLabel("Calorie Count: 0");
        leftPanel.add(calorieCountLabel, BorderLayout.NORTH);

        goalLabel = new JLabel("Daily Limit: " + dailyCalorieLimit + " calories");
        leftPanel.add(goalLabel, BorderLayout.SOUTH);

        dataset = new DefaultCategoryDataset();
        JFreeChart chart = ChartFactory.createBarChart(
                "Calorie Intake Statistics",
                "Food",
                "Calories",
                dataset
        );
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 300));
        leftPanel.add(chartPanel, BorderLayout.CENTER);

        mainPanel.add(leftPanel);

        pieDataset = new DefaultPieDataset();
        JFreeChart pieChart = ChartFactory.createPieChart(
                "Calorie Distribution",
                pieDataset,
                true,
                true,
                false
        );
        ChartPanel pieChartPanel = new ChartPanel(pieChart);
        pieChartPanel.setPreferredSize(new Dimension(400, 300));
        rightPanel.add(pieChartPanel, BorderLayout.CENTER);

        mainPanel.add(rightPanel);

        getContentPane().add(mainPanel, BorderLayout.CENTER);

        dayComboBox = new JComboBox<>();
        dayComboBox.addItem("Day 1");
        dayComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGraph();
            }
        });
        getContentPane().add(dayComboBox, BorderLayout.SOUTH);

        updateGraph();
    }

    private void setDailyCalorieLimit() {
        try {
            dailyCalorieLimit = Integer.parseInt(dailyLimitInput.getText());
            goalLabel.setText("Daily Limit: " + dailyCalorieLimit + " calories");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid input. Please enter a valid integer for the daily calorie limit.");
        }
    }

    private void addCalorieEntry() {
        try {
            String food = foodInput.getText();
            int calories = Integer.parseInt(calorieInput.getText());
            int selectedDayIndex = dayComboBox.getSelectedIndex();
            calorieLogs.get(selectedDayIndex).add(calories);
            Map<String, Integer> foodsForSelectedDay = foodLogs.get(selectedDayIndex);
            foodsForSelectedDay.put(food, calories);
            logTextArea.append("Calories logged for Day " + (selectedDayIndex + 1) + ": " + food + " - " + calories + " calories\n");
            updateCalorieCountLabel();
            updateGraph();
            updatePieChart();
            int totalCaloriesConsumed = calorieLogs.get(selectedDayIndex).stream().mapToInt(Integer::intValue).sum();
            int remainingCalories = dailyCalorieLimit - totalCaloriesConsumed;
            if (remainingCalories >= 0) {
                String[] messages = {
                        "Great job! Keep up the good work!",
                        "You're doing fantastic! Stay motivated!",
                        "One step closer to your goal! Keep pushing!",
                        "Every calorie counts! You're making progress!",
                        "You've got this! Keep making healthy choices!"
                };
                Random random = new Random();
                String message = messages[random.nextInt(messages.length)];
                JOptionPane.showMessageDialog(this, message + "\nRemaining calories to take: " + remainingCalories, "Motivation", JOptionPane.INFORMATION_MESSAGE);
            } else {
                int exceededCalories = Math.abs(remainingCalories);
                JOptionPane.showMessageDialog(this, "You've exceeded your daily calorie limit by " + exceededCalories + " calories!", "Daily Limit Exceeded", JOptionPane.WARNING_MESSAGE);
            }
            // Inserting data into MongoDB
            Document document = new Document("food", food).append("calories", calories).append("day", selectedDayIndex + 1);
            collection.insertOne(document);
            foodInput.setText("");
            calorieInput.setText("");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid input. Please enter a valid integer for calories.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCalorieCountLabel() {
        int totalCalories = 0;
        int selectedDayIndex = dayComboBox.getSelectedIndex();
        for (int calorie : calorieLogs.get(selectedDayIndex)) {
            totalCalories += calorie;
        }
        calorieCountLabel.setText("Total Calorie Count: " + totalCalories);
    }

    private void viewCalorieLog() {
        int selectedDayIndex = dayComboBox.getSelectedIndex();
        List<Integer> logsForSelectedDay = calorieLogs.get(selectedDayIndex);
        Map<String, Integer> foodsForSelectedDay = foodLogs.get(selectedDayIndex);
        if (!logsForSelectedDay.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> entry : foodsForSelectedDay.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(" calories\n");
            }
            JOptionPane.showMessageDialog(this, sb.toString(), "Calorie Log for Day " + (selectedDayIndex + 1), JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Calorie log for Day " + (selectedDayIndex + 1) + " is empty.", "Calorie Log", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void updateGraph() {
        dataset.clear();
        int selectedDayIndex = dayComboBox.getSelectedIndex();
        Map<String, Integer> foodsForSelectedDay = foodLogs.get(selectedDayIndex);
        for (Map.Entry<String, Integer> entry : foodsForSelectedDay.entrySet()) {
            dataset.addValue(entry.getValue(), "Calories", entry.getKey());
        }
    }

    private void updatePieChart() {
        pieDataset.clear();
        int selectedDayIndex = dayComboBox.getSelectedIndex();
        Map<String, Integer> foodsForSelectedDay = foodLogs.get(selectedDayIndex);
        for (Map.Entry<String, Integer> entry : foodsForSelectedDay.entrySet()) {
            pieDataset.setValue(entry.getKey(), entry.getValue());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new CalorieTrackerGUI().setVisible(true);
            }
        });
    }
}
