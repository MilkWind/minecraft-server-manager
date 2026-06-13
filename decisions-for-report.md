quesiton 1: for View server performance data: CPU, memory, network speed
answer: Remove all relevant logic for this feature on both the frontend and backend.

question 2: for Restart server after asset state changes
answer: Retain the existing logic for this feature. It is sufficient to only prompt administrators to restart the server; do not trigger automatic server restarts at the program layer once the asset status changes.

question 3: for Backend creates a session-bound authentication token
answer: Keep the current token generation and expiration logic. Session handling does not need to be taken into account (remove any existing session-related logic if present). Implement a logout function on both the frontend and backend.

question 4: for If the session changes or becomes invalid, the token is deprecated
answer: The solution is identical to that for question 3.

question 5: for Backend validates command identity where applicable
answer: Managers may input arbitrary commands for each server. These commands can be stored in the database with a dedicated remark in the UI for later invocation.

question 6: for Memory usage reflects server runtime and Network speed reflects server runtime
answer: Remove all relevant logic; the solution is the same as question 1.

question 7: for UI text uses Chinese
answer: Nearly all UI text is already localized to Chinese. Please conduct verification and update the file design-implementation-verification-report.md accordingly.