package com.techelevator.tenmo;

import com.techelevator.tenmo.model.Account;
import com.techelevator.tenmo.model.AuthenticatedUser;
import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.UserCredentials;
import com.techelevator.tenmo.services.AppService;
import com.techelevator.tenmo.services.AuthenticationService;
import com.techelevator.tenmo.services.ConsoleService;

import java.math.BigDecimal;
import java.util.List;

public class App {

    private static final String API_BASE_URL = "http://localhost:8080/";

    private final ConsoleService consoleService = new ConsoleService();
    private final AuthenticationService authenticationService = new AuthenticationService(API_BASE_URL);
    private final AppService appService = new AppService(API_BASE_URL);

    private AuthenticatedUser currentUser;

    public static void main(String[] args) {
        App app = new App();
        app.run();
    }

    private void run() {
        consoleService.printGreeting();
        loginMenu();
        if (currentUser != null) {
            mainMenu();
        }
    }
    private void loginMenu() {
        int menuSelection = -1;
        while (menuSelection != 0 && currentUser == null) {
            consoleService.printLoginMenu();
            menuSelection = consoleService.promptForMenuSelection("Please choose an option: ");
            if (menuSelection == 1) {
                handleRegister();
            } else if (menuSelection == 2) {
                handleLogin();
            } else if (menuSelection != 0) {
                System.out.println("Invalid Selection");
                consoleService.pause();
            }
        }
    }

    private void handleRegister() {
        System.out.println("Please register a new user account");
        UserCredentials credentials = consoleService.promptForCredentials();
        if (authenticationService.register(credentials)) {
            System.out.println("Registration successful. You can now login.");
        } else {
            consoleService.printErrorMessage();
        }
    }

    private void handleLogin() {
        UserCredentials credentials = consoleService.promptForCredentials();
        currentUser = authenticationService.login(credentials);
        if (currentUser == null) {
            consoleService.printErrorMessage();
        }
    }

    private void mainMenu() {
        int menuSelection = -1;
        while (menuSelection != 0) {
            consoleService.printMainMenu();
            menuSelection = consoleService.promptForMenuSelection("Please choose an option: ");
            if (menuSelection == 1) {
                viewCurrentBalance();
            } else if (menuSelection == 2) {
                viewTransferHistory();
//            } else if (menuSelection == 3) {
//                viewPendingRequests();
            } else if (menuSelection == 3) {
                sendBucks();
//            } else if (menuSelection == 5) {
//                requestBucks();
            } else if (menuSelection == 0) {
                continue;
            } else {
                System.out.println("Invalid Selection");
            }
            consoleService.pause();
        }
    }

    private void viewCurrentBalance() {
        System.out.println("Your current account balance is: $" +
                appService.getBalance(currentUser.getUser().getId(), currentUser.getToken()));
    }

    private void viewTransferHistory() {
        int transferChoice = -1;
        Account account = appService.getAccountById(currentUser.getUser().getId(), currentUser.getToken());
        List<Transfer> transfers =
                appService.getTransfersByAccountId(account.getAccountId(), currentUser.getToken());

        // No transfers found
        if (transfers.size() == 0) {
            System.out.println("\nNo transfers available.");
            return;
        }

        // Print transfers for account
        System.out.println(
                "\n-------------------------------------------\n" +
                "Transfers\n" +
                String.format("%-12s%-24s%7s\n", "ID", "From/To", "Amount") +
                "-------------------------------------------");

        // Loop through account transfers to populate display
        for (Transfer transfer : transfers) {
            long tId = transfer.getTransferId();
            String tDirection = (transfer.getAccountFrom() == account.getAccountId()) ? "To:" : "From:";
            String tParty = (tDirection.equals("To:")) ?
                    appService.getUsernameByAccountId(
                            transfer.getAccountTo(), currentUser.getToken()) :
                    appService.getUsernameByAccountId(
                            transfer.getAccountFrom(), currentUser.getToken());
            BigDecimal tMoney = transfer.getAmount();
            System.out.println(String.format("%-12s%-6s%-12s%13s", tId, tDirection, tParty, "$" + tMoney));
        }

        System.out.println("---------");

        // Transfer details option
        transferChoice = consoleService.promptForInt("Please enter transfer ID to view details (0 to cancel): ");
        if (transferChoice > 0) {

            // Retrieve transfer details
            Transfer selectedTransfer = null;
            for (Transfer transfer : transfers) {
                if(transferChoice == transfer.getTransferId()) {
                    selectedTransfer = transfer;
                    break;
                }
            }
            if(selectedTransfer == null) {
                System.out.println("\nInvalid ID");
                return;
            }

            // Print transfer details
            System.out.println(
                    "\n--------------------------------------------\n" +
                    "Transfer Details\n" +
                    "--------------------------------------------\n" +
                    String.format("%-8s%-36s\n", "Id:", selectedTransfer.getTransferId()) +
                    String.format("%-8s%-36s\n", "From:",
                            appService.getUsernameByAccountId(
                                    selectedTransfer.getAccountFrom(), currentUser.getToken())) +
                    String.format("%-8s%-36s\n", "To:",
                            appService.getUsernameByAccountId(
                                    selectedTransfer.getAccountTo(), currentUser.getToken())) +
                    String.format("%-8s%-36s\n", "Type:",
                            selectedTransfer.getTransferTypeId() == 1 ? "Request" : "Send") +
                    String.format("%-8s%-36s\n", "Status:",
                            selectedTransfer.getTransferStatusId() == 1 ? "Pending" :
                                    selectedTransfer.getTransferStatusId() == 2 ? "Approved" : "Rejected") +
                    String.format("%-8s%-36s", "Amount:", "$" + selectedTransfer.getAmount()));
            System.out.println("---------");
        } else if (transferChoice == 0) {
            return;
        } else {
            System.out.println("Invalid selection");
        }
    }

    private void sendBucks() {
        Account currentAccount =
                appService.getAccountById(currentUser.getUser().getId(), currentUser.getToken());
        String searchTerm = consoleService.promptForString(
                "Please enter the username you'd like to send TEnmo Bucks to: ");

        List<Long> accountIds =
                appService.getAccountIdsByUsernameSearch(searchTerm, currentUser.getToken());

        // No account found
        if (accountIds.size() == 0) {
            System.out.println("\nNo account was found.");
            return;
        }

        // Print list of account numbers with usernames
        System.out.println("-------------------------------------------\n" +
                "Account\n" +
                "ID          Name\n" +
                "-------------------------------------------");

        for (Long accountId : accountIds) {
            System.out.println(accountId + "     " +
                    (appService.getUsernameByAccountId(accountId, currentUser.getToken())));
        }
        System.out.println("---------");

        // Select recipient account
        long accountSelection = consoleService.promptForInt(
                "Enter ID of account you are sending to (0 to cancel):");

        Long targetAccountId = null;
        for(Long accountId : accountIds) {
            if(accountId == accountSelection) {
                targetAccountId = accountId;
                break;
            }
        }

        if(targetAccountId == null) {
            System.out.println("No account was selected.");
            return;
        }
        if(targetAccountId == currentAccount.getAccountId()) {
            System.out.println("Self-selection is not permitted.");
            return;
        }

        // Prompt for transfer amount
        BigDecimal transferAmount = consoleService.promptForBigDecimal("Enter amount:");
        if(transferAmount.compareTo(BigDecimal.valueOf(0.00)) <= 0) {
            System.out.println("Transfer canceled (amount must be greater than 0.00)");
            return;
        }

        // Check for availability of funds in currentUser's account
        if (currentAccount.getBalance().compareTo(transferAmount) < 0) {
            System.out.println("You don't have enough TE Bucks.");
            return;
        }

        // Set transfer details
        Transfer transfer = new Transfer();
        transfer.setTransferTypeId(2);
        transfer.setTransferStatusId(2);
        transfer.setAccountFrom(currentAccount.getAccountId());
        transfer.setAccountTo(targetAccountId);
        transfer.setAmount(transferAmount);

        // Create transfer
        transfer = appService.createTransfer(transfer, currentUser.getToken());

        // Print approved transaction details
        System.out.println("\n--------------------------------------------\n" +
                "Transfer Details\n" +
                "--------------------------------------------\n" +
                " Id: " + transfer.getTransferId() + "\n" +
                " From: " + currentUser.getUser().getUsername() + "\n" +
                " To: " + appService.getUsernameByAccountId(targetAccountId, currentUser.getToken()) + "\n" +
                " Type: Send" + "\n" +
                " Status: Approved" + "\n" +
                " Amount: " + transfer.getAmount() +
                "\n---------\n"
        );
    }
}
