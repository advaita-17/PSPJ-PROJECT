// Project: Adaptive Quiz and Exam Engine
// Team: Advaita (2620040161), Bhavya (2620040177), Chinmayee (2620040174)

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Adaptive Quiz and Exam Engine
 * CO1: problem specification, deterministic input-process-output flow
 * CO2: if/switch, loops, flags and input decisions
 * CO3: methods, arrays, arguments/returns, recursion
 * CO4: classes, objects, encapsulation, interface, exceptions
 * CO5: Strings, sorting, file persistence, parsing
 * CO6: collections, lambdas, functional interfaces and streams
 */
interface Scorable {
    boolean checkAnswer(QuizQuestion question, String userAnswer);
}

class QuizQuestion {
    private final String text;
    private final String answer;
    private final String difficulty;

    public QuizQuestion(String text, String answer) {
        this(text, answer, "Medium");
    }

    public QuizQuestion(String text, String answer, String difficulty) {
        this.text = text;
        this.answer = answer;
        this.difficulty = difficulty;
    }

    public String getText() { return text; }
    public String getAnswer() { return answer; }
    public String getDifficulty() { return difficulty; }

    @Override
    public String toString() {
        return "[" + difficulty + "] " + text;
    }
}

class QuizResult {
    private final String question;
    private final String difficulty;
    private final String userAnswer;
    private final boolean correct;

    public QuizResult(String question, String difficulty, String userAnswer, boolean correct) {
        this.question = question;
        this.difficulty = difficulty;
        this.userAnswer = userAnswer;
        this.correct = correct;
    }

    public String getQuestion() { return question; }
    public String getDifficulty() { return difficulty; }
    public String getUserAnswer() { return userAnswer; }
    public boolean isCorrect() { return correct; }

    @Override
    public String toString() {
        return difficulty + " | " + question + " | Your answer: " + userAnswer
                + " | " + (correct ? "Correct" : "Incorrect");
    }
}

class InvalidQuizInputException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidQuizInputException(String message) {
        super(message);
    }
}

class QuizStudent {
    private final String name;
    private int score;

    public QuizStudent(String name) {
        String cleaned = name == null ? "" : name.trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Student name cannot be empty.");
        }
        this.name = cleaned;
        this.score = 0;
    }

    public String getName() { return name; }
    public int getScore() { return score; }
    public void addPoint() { score++; }

    @Override
    public String toString() {
        return name + " (score: " + score + ")";
    }
}

public class AdaptiveQuizEngine implements Scorable {
    private static final int TOTAL_QUESTIONS = 5;
    private static final String REPORT_FILE = "quiz_report.txt";

    private final Scanner scanner = new Scanner(System.in);
    private final List<QuizResult> results = new ArrayList<>();
    private final Map<String, List<QuizQuestion>> questionBank = new HashMap<>();
    private final Set<String> uniqueQuestionTexts = new HashSet<>();
    private final Map<String, Integer> scoreByDifficulty = new TreeMap<>();
    private QuizStudent student;

    public AdaptiveQuizEngine() {
        // CO3: arrays are a simple fixed-size way to define starter question data.
        QuizQuestion[] easy = {
            new QuizQuestion("What is 2 + 2?", "4", "Easy"),
            new QuizQuestion("What is 5 - 3?", "2", "Easy"),
            new QuizQuestion("How many sides does a triangle have?", "3", "Easy")
        };
        QuizQuestion[] medium = {
            new QuizQuestion("What is 6 x 7?", "42", "Medium"),
            new QuizQuestion("What is 9 + 8?", "17", "Medium"),
            new QuizQuestion("What is 20 / 4?", "5", "Medium")
        };
        QuizQuestion[] hard = {
            new QuizQuestion("What is 12 x 12?", "144", "Hard"),
            new QuizQuestion("What is 144 / 12?", "12", "Hard"),
            new QuizQuestion("What is 15 squared?", "225", "Hard")
        };

        // CO6: List provides a resizable collection; Map looks up a level by name.
        questionBank.put("Easy", new ArrayList<>(Arrays.asList(easy)));
        questionBank.put("Medium", new ArrayList<>(Arrays.asList(medium)));
        questionBank.put("Hard", new ArrayList<>(Arrays.asList(hard)));

        // CO6: Set stores unique question text; TreeMap keeps summary keys ordered.
        for (List<QuizQuestion> questions : questionBank.values()) {
            for (QuizQuestion q : questions) {
                uniqueQuestionTexts.add(q.getText());
            }
        }
        scoreByDifficulty.put("Easy", 0);
        scoreByDifficulty.put("Medium", 0);
        scoreByDifficulty.put("Hard", 0);
    }

    @Override
    public boolean checkAnswer(QuizQuestion question, String userAnswer) {
        // CO4: interface contract; CO5: compare String values rather than references.
        return question != null && userAnswer != null
                && question.getAnswer().equalsIgnoreCase(userAnswer.trim());
    }

    private String nextDifficulty(String current, boolean correct) {
        // CO2: switch maps each state to its next state.
        if (correct) {
            switch (current) {
                case "Easy": return "Medium";
                case "Medium": return "Hard";
                default: return "Hard";
            }
        } else {
            switch (current) {
                case "Hard": return "Medium";
                case "Medium": return "Easy";
                default: return "Easy";
            }
        }
    }

    private QuizQuestion getQuestion(String difficulty, int questionNumber) {
        List<QuizQuestion> levelQuestions = questionBank.getOrDefault(difficulty,
                questionBank.get("Easy"));
        return levelQuestions.get((questionNumber - 1) % levelQuestions.size());
    }

    private String readAnswer() throws InvalidQuizInputException {
        System.out.print("Your answer: ");
        String answer = scanner.nextLine().trim();
        if (answer.isEmpty()) {
            throw new InvalidQuizInputException("Answer cannot be blank. This attempt will be marked incorrect.");
        }
        return answer;
    }

    private void runQuiz() {
        System.out.println("\n=== ADAPTIVE QUIZ ===");
        System.out.print("Enter student name: ");
        String name = scanner.nextLine();
        try {
            student = new QuizStudent(name);
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage() + " Using 'Student' as the name.");
            student = new QuizStudent("Student");
        }

        String difficulty = "Medium";
        for (int i = 1; i <= TOTAL_QUESTIONS; i++) { // CO2: definite for-loop
            QuizQuestion question = getQuestion(difficulty, i);
            System.out.println("\nQuestion " + i + " of " + TOTAL_QUESTIONS
                    + "  |  Level: " + difficulty);
            System.out.println(question.getText());

            String answer;
            boolean correct = false;
            try {
                answer = readAnswer();
                correct = checkAnswer(question, answer); // CO4 interface + CO5 String comparison
            } catch (InvalidQuizInputException ex) {
                System.out.println(ex.getMessage());
                answer = "(blank)";
            }

            if (correct) {
                System.out.println("Correct!");
                student.addPoint();
                scoreByDifficulty.put(difficulty, scoreByDifficulty.get(difficulty) + 1);
            } else {
                System.out.println("Not quite. Correct answer: " + question.getAnswer());
            }

            results.add(new QuizResult(question.getText(), difficulty, answer, correct));
            difficulty = nextDifficulty(difficulty, correct);
        }
    }

    private void printSummary() {
        System.out.println("\n=== QUIZ SUMMARY ===");
        System.out.println(student);
        System.out.println("Unique questions in bank: " + uniqueQuestionTexts.size());

        // CO6: Predicate and stream filter express the intent declaratively.
        Predicate<QuizResult> correctResult = QuizResult::isCorrect;
        List<QuizResult> correctAnswers = results.stream()
                .filter(correctResult)
                .collect(Collectors.toList());
        long correctCount = results.stream().filter(QuizResult::isCorrect).count();
        System.out.println("Correct answers: " + correctCount + " / " + results.size());

        // CO5: reusable library sort; CO6: stream sorted + method reference.
        Comparator<QuizResult> byQuestion = Comparator.comparing(QuizResult::getQuestion);
        List<QuizResult> sorted = results.stream().sorted(byQuestion).collect(Collectors.toList());
        System.out.println("\nResults sorted by question:");
        sorted.forEach(System.out::println);

        // CO6: reduce aggregates a list; Optional safely represents a possibly absent result.
        Optional<QuizResult> firstCorrect = correctAnswers.stream().findFirst();
        System.out.println("\nFirst correct answer: "
                + firstCorrect.map(QuizResult::getQuestion).orElse("No correct answers yet"));

        // CO6: Consumer represents an action that accepts each result.
        Consumer<QuizResult> showCorrect = r -> System.out.println("✓ " + r.getQuestion());
        System.out.println("\nCorrect question list:");
        correctAnswers.forEach(showCorrect);

        int scoreFromStream = results.stream()
                .filter(QuizResult::isCorrect)
                .map(r -> 1)
                .reduce(0, Integer::sum);
        System.out.println("\nStream-calculated score: " + scoreFromStream);

        System.out.println("Score by difficulty: " + scoreByDifficulty);
        System.out.println("Digit sum of score (recursive CO3 demo): " + sumDigits(student.getScore()));
    }

    // CO3: overloaded methods accept different parameter lists.
    private String formatScore(int score) {
        return "Score: " + score;
    }

    private String formatScore(int score, int total) {
        return "Score: " + score + "/" + total;
    }

    private void demonstrateLibraryConcepts() {
        System.out.println("\n=== EXTRA JAVA CONCEPT DEMOS ===");

        // CO3: two-dimensional arrays and nested loops.
        int[][] sampleGrid = {{1, 2}, {3, 4}};
        int gridSum = 0;
        for (int[] row : sampleGrid) {
            for (int value : row) gridSum += value;
        }
        System.out.println("2D array total: " + gridSum);

        // CO5: standard array sorting and binary search.
        int[] sampleNumbers = {9, 2, 5, 1};
        Arrays.sort(sampleNumbers);
        System.out.println("Sorted numbers: " + Arrays.toString(sampleNumbers));
        System.out.println("Position of 5: " + Arrays.binarySearch(sampleNumbers, 5));

        // CO5: StringBuilder, split and join for text processing.
        StringBuilder label = new StringBuilder("Quiz");
        label.append(" Report");
        String[] parts = label.toString().split("\\s+");
        System.out.println("Text tokens: " + String.join(" / ", parts));

        // CO6: Supplier provides a value; Function transforms a value.
        Supplier<String> statusSupplier = () -> "Report ready";
        Function<String, Integer> wordCounter = text ->
                text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
        System.out.println(statusSupplier.get() + "; words in label: " + wordCounter.apply(label.toString()));

        // CO6: Iterator safely removes an item during iteration.
        Set<String> temporarySet = new HashSet<>(Arrays.asList("Easy", "Medium", "Hard"));
        Iterator<String> iterator = temporarySet.iterator();
        if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
        System.out.println("Set after iterator removal: " + temporarySet);

        // CO6: Queue represents first-in-first-out work.
        Queue<String> reportTasks = new ArrayDeque<>();
        reportTasks.add("Calculate score");
        reportTasks.add("Save report");
        System.out.println("Next task in queue: " + reportTasks.poll());

        // CO3: method overloading.
        System.out.println(formatScore(student.getScore()));
        System.out.println(formatScore(student.getScore(), TOTAL_QUESTIONS));
    }

    // CO3: recursion with a base case and a smaller recursive call.
    private int sumDigits(int number) {
        if (number < 10) return number;
        return number % 10 + sumDigits(number / 10);
    }

    private void saveReport() {
        Path path = Paths.get(REPORT_FILE);
        List<String> lines = new ArrayList<>();
        lines.add("Adaptive Quiz and Exam Engine");
        lines.add("Student: " + student.getName());
        lines.add("Score: " + student.getScore() + "/" + TOTAL_QUESTIONS);
        lines.add("Question|Difficulty|YourAnswer|Correct");

        // CO5: pipe-delimited records make the text report easy to inspect.
        for (QuizResult r : results) {
            String safeQuestion = r.getQuestion().replace("|", "/");
            String safeAnswer = r.getUserAnswer().replace("|", "/");
            lines.add(safeQuestion + "|" + r.getDifficulty() + "|" + safeAnswer + "|" + r.isCorrect());
        }

        try {
            Files.write(path, lines, StandardCharsets.UTF_8);
            System.out.println("\nReport saved to: " + path.toAbsolutePath());
            readReport(path);
        } catch (IOException ex) {
            System.out.println("Could not save/read the report: " + ex.getMessage());
        }
    }

    private void readReport(Path path) throws IOException {
        System.out.println("\n=== REPORT READ BACK FROM FILE ===");
        // CO5/CO6: Files.lines creates a stream; try-with-resources closes it safely.
        try (Stream<String> fileLines = Files.lines(path, StandardCharsets.UTF_8)) {
            fileLines.forEach(System.out::println);
        }
    }

    private void showMenuDemo() {
        // CO2: switch statement selects one of the known actions.
        System.out.println("\nReport status:");
        switch (results.isEmpty() ? 0 : 1) {
            case 1:
                System.out.println("The quiz has results ready for reporting.");
                break;
            default:
                System.out.println("No quiz results available.");
        }
    }

    public static void main(String[] args) {
        AdaptiveQuizEngine engine = new AdaptiveQuizEngine();
        engine.runQuiz();
        engine.printSummary();
        engine.saveReport();
        engine.showMenuDemo();
        engine.demonstrateLibraryConcepts();
        engine.scanner.close();
    }
}
