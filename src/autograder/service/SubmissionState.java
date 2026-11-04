package autograder.service;

import java.util.ArrayList;

/** Tracks issues, corrections and warnings for one submission only. */
enum Stage { NONE, STRUCTURE, VALIDATION, HEADERS }

    class SubmissionState {
        Stage stage = Stage.NONE;
        boolean structureCorrected;
        boolean headerCorrected;
        final ArrayList<String> notableMessages = new ArrayList<>();

        // store issues for details csv
        final ArrayList<String> submissionIssues = new ArrayList<>();
        final ArrayList<String> q1Issues = new ArrayList<>();
        final ArrayList<String> q2Issues = new ArrayList<>();
        final ArrayList<String> q3Issues = new ArrayList<>();
        final ArrayList<String> folderFixes = new ArrayList<>();
        final ArrayList<String> nameFixes = new ArrayList<>();
        final ArrayList<String> emailFixes = new ArrayList<>();
        final ArrayList<String> openIssues = new ArrayList<>();
        final ArrayList<String> anomalies = new ArrayList<>();

        void addSubmissionIssue(String issue) {
            if (!submissionIssues.contains(issue)) {
                submissionIssues.add(issue);
            }
        }

        void addQuestionIssue(String questionId, String issue) {
            ArrayList<String> target;
            if (questionId.startsWith("Q1")) {
                target = q1Issues;
            } else if (questionId.startsWith("Q2")) {
                target = q2Issues;
            } else if (questionId.startsWith("Q3")) {
                target = q3Issues;
            } else {
                return;
            }

            if (!target.contains(issue)) {
                target.add(issue);
            }
        }

        void addOpenIssue(String issue) {
            if (!openIssues.contains(issue)) openIssues.add(issue);
        }

        void addAnomaly(String anomaly) {
            if (!anomalies.contains(anomaly)) anomalies.add(anomaly);
        }

        void addFix(ArrayList<String> target, String detail) {
            if (!target.contains(detail)) target.add(detail);
        }

        String stripCorrectedPrefix(String text) {
            return text.replaceFirst("^\\[CORRECTED\\]\\s*", "").trim();
        }

        String compactFolderFix(String detail) {
            if (detail.startsWith("Renamed top-level folder from ")) {
                String rest = detail.substring("Renamed top-level folder from ".length());
                return rest.replace(" to ", " -> ");
            }
            if (detail.startsWith("Added missing username folder: ")) {
                return "Added username folder: " + detail.substring("Added missing username folder: ".length());
            }
            return detail;
        }

        String compactHeaderFix(String detail, String label) {
            if (detail.startsWith("Added " + label + " to ")) {
                return detail.substring(("Added " + label + " to ").length()) + " (added)";
            }
            if (label.equals("Email ID") && detail.startsWith("Email ID in ")) {
                String rest = detail.substring("Email ID in ".length());
                int colon = rest.lastIndexOf(':');
                String path = colon >= 0 ? rest.substring(0, colon).trim() : rest;
                return path + " (full email changed to short ID)";
            }
            return detail;
        }

        void track(String message) {
            String text = message.trim();
            if (text.startsWith("[CORRECTED]")) {
                String detail = stripCorrectedPrefix(text);
                if (stage == Stage.STRUCTURE) {
                    structureCorrected = true;
                    addFix(folderFixes, compactFolderFix(detail));
                }
                if (stage == Stage.HEADERS) {
                    headerCorrected = true;
                    if (detail.contains("Name")) addFix(nameFixes, compactHeaderFix(detail, "Name"));
                    if (detail.contains("Email ID")) addFix(emailFixes, compactHeaderFix(detail, "Email ID"));
                }
                notableMessages.add(text);
            } else if (text.startsWith("[WARNING]") || text.startsWith("[MISSING]") || text.startsWith("[ERROR]")) {
                notableMessages.add(text);
            }
        }
    }
