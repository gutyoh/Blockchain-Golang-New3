import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.hyperskill.hstest.testcase.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


class BlockParseException extends Exception {
    BlockParseException(String msg) {
        super(msg);
    }
}


class Block {

    int id;
    long timestamp;
    long magic;
    String prevHash;
    String hash;
    String prevTxId;

    String txId;

    static ArrayList<String> minerIds;
    static int N;

    static Block parseBlock(String strBlock) throws BlockParseException {
        if (strBlock.length() == 0) {
            return null;
        }

        if (!(strBlock.toLowerCase().contains("block")
                && strBlock.toLowerCase().contains("timestamp"))) {

            return null;
        }

        Block block = new Block();

        // Get the Block ID from strBlock
        for (String line : strBlock.split("\n")) {
            if (line.toLowerCase().startsWith("id:")) {
                String id = line.split(":")[1].strip().replace("-", "");
                boolean isNumeric = id.chars().allMatch(Character::isDigit);

                if (!isNumeric) {
                    throw new BlockParseException("Id should be a number");
                }
                block.id = Integer.parseInt(id);
                break;
            }
        }

        List<String> lines = strBlock
                .lines()
                .map(String::strip)
                .filter(e -> e.length() > 0)
                .collect(Collectors.toList());

        if (lines.size() < 12) {
            throw new BlockParseException("Every block should contain at least 12 lines of data");
        }

        // Check the `Block data` of the `Genesis Block`:
        if (block.id == 1) {
            if (!lines.get(0).toLowerCase().startsWith("genesis block")) {
                throw new BlockParseException("The first line of the first block in the blockchain should be \"Genesis Block:\" and every subsequent Block's first line should be \"Block:\"" +
                        "\nYour program instead printed as the first line in Block " + block.id + ": " + "\"" + lines.get(0) + "\"");
            }

            if (!lines.get(1).toLowerCase().startsWith("id:")) {
                throw new BlockParseException("Second line of the Genesis Block should start with \"Id:\"");
            }

            if (!lines.get(2).toLowerCase().startsWith("timestamp:")) {
                throw new BlockParseException("Third line of the Genesis Block should start with \"Timestamp:\"");
            }
            String timestamp = lines.get(2).split(":")[1].strip().replace("-", "");
            boolean isNumeric = timestamp.chars().allMatch(Character::isDigit);

            if (!isNumeric) {
                throw new BlockParseException("Timestamp should be a number");
            }
            block.timestamp = Long.parseLong(timestamp);

            if (!lines.get(3).toLowerCase().startsWith("magic number:")) {
                throw new BlockParseException("4-th line of the Genesis Block should start with \"Magic number:\"");
            }
            String magic = lines.get(3).split(":")[1].strip().replace("-", "");
            isNumeric = magic.chars().allMatch(Character::isDigit);

            if (!isNumeric) {
                throw new BlockParseException("Magic number should be a number");
            }
            block.magic = Long.parseLong(magic);

            if (!lines.get(4).equalsIgnoreCase("hash of the previous block:")) {
                throw new BlockParseException("5-th line of the Genesis Block should start with \"Hash of the previous block:\"");
            }

            if (!lines.get(6).equalsIgnoreCase("hash of the block:")) {
                throw new BlockParseException("7-th line of the Genesis Block should start with \"Hash of the block:\"");
            }

            String prevHash = lines.get(5).strip();
            String hash = lines.get(7).strip();

            if (!(prevHash.length() == 64 || prevHash.equals("0")) || hash.length() != 64) {
                throw new BlockParseException("Hash length should be equal to 64 except \"0\"");
            }

            if (hash.equals(prevHash)) {
                throw new BlockParseException("The current hash and the previous hash in a block should be different.");
            }

            if (!hash.startsWith("0".repeat(N))) {
                throw new BlockParseException("N is " + N + " but hash, " + hash + ", did not start with the correct number of zeros.");
            }
            block.hash = hash;
            block.prevHash = prevHash;

            if (!lines.get(8).toLowerCase().startsWith("block data:")) {
                throw new BlockParseException("9-th line of the Genesis Block " +
                        "should start with \"Block data:\""

                        + "\n" + "Your program instead printed in Block " +
                        block.id + " an unexpected line: " + lines.get(9));
            }

            if (!lines.get(9).toLowerCase().contains("no transactions")) {
                throw new BlockParseException("10-th line of the Genesis Block " +
                        "should be \"No transactions\"");
            }

            if (!(lines.get(10).toLowerCase().contains("block") || lines.get(10).toLowerCase().contains("generating"))) {
                throw new BlockParseException("11-th line of the Genesis Block " +
                        "should say how long the block was generating for! "
                        + "(Use the example's format)"

                        + "\n" + "Your program instead printed in Block " +
                        block.id + " an unexpected line: " + lines.get(10));
            }

            if (!lines.get(11).toUpperCase().startsWith("N ")) {
                throw new BlockParseException("12-th line of the Genesis Block " +
                        "should be state what happened to N in the format given."

                        + "\n" + "Your program instead printed in Block " +
                        block.id + " an unexpected line: " + lines.get(11));
            }

            if (lines.get(11).toLowerCase().contains("increase")) {
                N += 1;
            } else if (lines.get(11).toLowerCase().contains("decrease")) {
                N -= 1;
                if (N < 0) {
                    throw new BlockParseException("N was decreased below zero!");
                }
            } else if (!lines.get(11).toLowerCase().contains("same")) {
                throw new BlockParseException("The last line of every block" +
                        "must state N increased, decreased, or stayed the same.");
            }

            if (11 != lines.size() - 1) {
                throw new BlockParseException("Your program printed in Block " + block.id +
                        " after the line: \"N was increased/decreased/stays the same\"\n" +
                        "an additional and unexpected line: " + lines.get(lines.size() - 1));
            }
        }

        // Check the `Block data of the subsequent blocks:`
        if (block.id > 1 && block.id <= 5) {
            if (!lines.get(0).toLowerCase().startsWith("block")) {
                throw new BlockParseException("The first line of the first block in the blockchain should be \"Genesis Block:\" and every subsequent Block's first line should be \"Block:\"" +
                        "\nYour program instead printed as the first line in Block " + block.id + ": " + "\"" + lines.get(0) + "\"");
            }

            if (!lines.get(1).toLowerCase().startsWith("created by")) {
                throw new BlockParseException("Second line of every subsequent Block should start with \"Created by\"");
            }
            minerIds.add(lines.get(1));

            if (!lines.get(3).toLowerCase().startsWith("timestamp:")) {
                throw new BlockParseException("4-th line of every subsequent Block should start with \"Timestamp:\"");
            }
            String timestamp = lines.get(3).split(":")[1].strip().replace("-", "");
            boolean isNumeric = timestamp.chars().allMatch(Character::isDigit);

            if (!isNumeric) {
                throw new BlockParseException("Timestamp should be a number");
            }
            block.timestamp = Long.parseLong(timestamp);

            if (!lines.get(4).toLowerCase().startsWith("magic number:")) {
                throw new BlockParseException("5-th line of every subsequent Block should start with \"Magic number:\"");
            }
            String magic = lines.get(4).split(":")[1].strip().replace("-", "");
            isNumeric = magic.chars().allMatch(Character::isDigit);

            if (!isNumeric) {
                throw new BlockParseException("Magic number should be a number");
            }
            block.magic = Long.parseLong(magic);

            if (!lines.get(5).equalsIgnoreCase("hash of the previous block:")) {
                throw new BlockParseException("6-th line of every subsequent Block should start with \"Hash of the previous block:\"");
            }

            if (!lines.get(7).equalsIgnoreCase("hash of the block:")) {
                throw new BlockParseException("8-th line of every subsequent Block should start with \"Hash of the block:\"");
            }
            String prevHash = lines.get(6).strip();
            String hash = lines.get(8).strip();

            if (!(prevHash.length() == 64 || prevHash.equals("0")) || hash.length() != 64) {
                throw new BlockParseException("Hash length should be equal to 64 except \"0\"");
            }

            if (hash.equals(prevHash)) {
                throw new BlockParseException("The current hash and the previous hash in a block should be different.");
            }

            if (!hash.startsWith("0".repeat(N))) {
                throw new BlockParseException("N is " + N + " but hash, " + hash + ", did not start with the correct number of zeros.");
            }
            block.hash = hash;
            block.prevHash = prevHash;

            if (!lines.get(9).toLowerCase().startsWith("block data:")) {
                throw new BlockParseException("10-th line of every subsequent Block should start with \"Block data:\""

                        + "\n" + "Your program instead printed in Block " +
                        block.id + " an unexpected line: " + lines.get(9));
            }

            int i = 10;

            while (!lines.get(i).toLowerCase().startsWith("transaction")) {
                i++;
            }

            // After the loop, the first line we reach should be
            // the line with the coinbase transaction — `Transaction #1 (Coinbase):`
            if (!lines.get(i).toLowerCase().startsWith("transaction")) {
                throw new BlockParseException("After the line with \"Block data:\" " +
                        "the next line should contain the coinbase transaction "
                        + "and start with \"Transaction\""

                        + "\n" + "Your program instead printed in Block " +
                        block.id + " an unexpected line: " + lines.get(i));
            }

            if (!lines.get(i).toLowerCase().contains("(coinbase)") || !lines.get(i).toLowerCase().contains("coinbase")) {
                throw new BlockParseException("After the line with \"Block data:\" " +
                        "the next line should contain \"(Coinbase)\" e.g., \"Transaction #1 (Coinbase):\""

                        + "\n" + "Your program instead printed in Block " +
                        block.id + " an unexpected line: " + lines.get(i));
            }

            i += 1;  // Get the line — `Transaction ID: <Transaction ID>`

            if (!lines.get(i).toLowerCase().startsWith("transaction id")) {
                throw new BlockParseException("After the line with the coinbase transaction " +
                        "the next line should start with \"Transaction ID:\""

                        + "\n" + "Your program instead printed in Block " +
                        block.id + " an unexpected line: " + lines.get(i));
            }

            // Get the Transaction ID value after the `:` colon in the string `Transaction ID:`
            String txId = lines.get(i).split(":")[1].strip();

            // Check if the Transaction ID is a double SHA256 hash
            if (!(txId.length() == 64 && txId.matches("^[a-zA-Z0-9]*$"))) {
                throw new BlockParseException("Transaction ID should be a double SHA256 hash created using the "
                        + "\"From user\", \"To user\" and \"VC Amount\" values of the transaction."
                        + "Your program instead printed in Block " + block.id +
                        " an incorrect Transaction ID: " + txId);
            }

            i += 1;  // Get the line — `Blockchain sent 100 VC to miner`

            if (!lines.get(i).toLowerCase().startsWith("blockchain sent 100 vc")) {
                throw new BlockParseException("After the line with the coinbase transaction ID " +
                        "the next line should start with \"Blockchain sent 100 VC\" followed by the name of the miner"
                        + " who mined the block, e.g., \"Blockchain sent 100 VC to miner1\""

                        + "\n" + "Your program instead printed in Block " +
                        block.id + " an unexpected line: " + lines.get(i));
            }

            i += 1;  // Get the line — `Public Key: <Public Key>`

            if (!lines.get(i).toLowerCase().startsWith("public key")) {
                throw new BlockParseException("After the line with the coinbase transaction ID " +
                        "the next line should start with \"Public Key:\""

                        + "\n" + "Your program instead printed in Block " +
                        block.id + " an unexpected line: " + lines.get(i));
            }

            // Get the Public Key value after the `:` colon in the string `Public Key:`
            String publicKey = lines.get(i).split(":")[1].strip();

            if (publicKey == null || publicKey.length() == 0) {
                throw new BlockParseException("Make sure you write the public key after the \"Public Key:\" string.\n" +
                        "For example \"Public Key: MFkw...\"");
            }

            // Check if the Public Key starts with `MF` and has a length of 120 or 124 characters
            if (!publicKey.startsWith("MF") || (publicKey.length() != 120 && publicKey.length() != 124)) {
                throw new BlockParseException("The Public Key should be in the PKIX, ASN.1 DER form and have a "
                        + "length of 120 or 124 characters.\n" +
                        "Your Public Key: " + publicKey + "\n" +
                        "Your Public Key length: " + publicKey.length());
            }

            // Now we move to the NON-COINBASE transactions
            i += 1;  // Get the line — `Transaction #2:`
            int txCounter = 2;

            while (!lines.get(i).toLowerCase().startsWith("block was generating")) {

                if (!lines.get(i).toLowerCase().startsWith("transaction #" + txCounter)) {
                    throw new BlockParseException("After the line with the coinbase transaction " +
                            "the next line should start with " +
                            "\"Transaction #" + txCounter + ":\"" + "\n" +
                            "Your program instead printed in Block " + block.id +
                            " an incorrect line: " + lines.get(i));
                }

                // Get the transaction number:
                int txNumber = 0;
                // 1. Check if there is a number after the `#` sign and no other characters "Transaction #2"
                if (lines.get(i).split("#")[1].matches("^[0-9]*$")) {
                    txNumber = Integer.parseInt(lines.get(i).split("#")[1]);
                } else if (lines.get(i).split("#")[1].split(":")[0].matches("^[0-9]*$")) {
                    txNumber = Integer.parseInt(lines.get(i).split("#")[1].split(":")[0]);
                } else {
                    throw new BlockParseException("After the line with the coinbase transaction " +
                            "the next line should start with \"Transaction #" + txCounter + ":\"" + "\n" +
                            "Your program instead printed in Block " + block.id + " an incorrect line: " + lines.get(i));
                }

                i += 1;  // Get the line — `Transaction ID: <Transaction ID>`

                if (!lines.get(i).toLowerCase().startsWith("transaction id")) {
                    throw new BlockParseException("After the line with the coinbase transaction " +
                            "the next line should start with \"Transaction ID:\"");
                }

                // Get the Transaction ID value after the `:` colon in the string `Transaction ID:`
                txId = lines.get(i).split(":")[1].strip();

                // Check if the Transaction ID is a double SHA256 hash
                if (!(txId.length() == 64 && txId.matches("^[a-zA-Z0-9]*$"))) {
                    throw new BlockParseException("Transaction ID should be a double SHA256 hash created using the "
                            + "\"From user\", \"To user\" and \"VC Amount\" values of the transaction."
                            + "Your program instead printed in Block " + block.id +
                            " an incorrect Transaction ID: " + txId);
                }

                // If there is more than two transactions (coinbase + 2 non-coinbase transactions)
                // Then check if the current transaction ID is different from the previous transaction ID
                if (txNumber > 2 && block.prevTxId.equals(txId)) {
                    throw new BlockParseException("The current transaction ID should be different from the previous transaction ID.\n" +
                            "Your program instead printed in Block " + block.id +
                            " the same transaction ID for two consecutive transactions: " +
                            " Previous transaction ID: " + block.prevTxId + "\n" +
                            " Current transaction ID: " + txId);
                }

                block.prevTxId = txId;

                i += 1;  // Get the line — `<From user> sent <VC Amount> VC to <To user>`

                if (!(lines.get(i).toLowerCase().contains("sent") && lines.get(i).toLowerCase().contains("vc") &&
                        lines.get(i).toLowerCase().contains("to"))) {
                    throw new BlockParseException("After the line with \"Transaction ID\" " +
                            "the next line should contain the transaction details, e.g., \"Nick sent 10 VC to Alice\"");
                }

                i += 1;  // Get the line — `Signature: <Signature>`

                if (!lines.get(i).toLowerCase().startsWith("signature")) {
                    throw new BlockParseException("After the line with the transaction details, e.g., \"Nick sent 10 VC to Alice\" " +
                            "the next line should contain the transaction signature "
                            + "and start with \"Signature:\" " +
                            "followed by the transaction signature."

                            + "\n" + "Your program instead printed in Block " +
                            block.id + " an unexpected line: " + lines.get(i));
                }

                // Get the signature value after the `:` colon in the string `Signature:`
                String signature = lines.get(i).split(":")[1].strip();

                if (signature == null || signature.length() == 0) {
                    throw new BlockParseException("Make sure you write the signature after the `Signature:` string.\n" +
                            "For example: \"Signature: MEUCIBFU...\"");
                }

                // Check if the signature starts with `ME` and has a length of 96 characters
                if (!signature.startsWith("ME") || (signature.length() != 96)) {
                    throw new BlockParseException("The Signature should be ASN.1 encoded and have a "
                            + "length of 96 characters.\n" +
                            "Your Signature: " + signature + "\n" +
                            "Your Signature length: " + signature.length());
                }

                i += 1;  // Get the line — `Public Key: <Public Key>`

                if (!lines.get(i).toLowerCase().startsWith("public key")) {
                    throw new BlockParseException("After the line with \"Signature:\" " +
                            "the next line should contain the public key "
                            + "and start with \"Public Key:\" " +

                            "\n" + "Your program instead printed in Block " +
                            block.id + " an unexpected line: " + lines.get(i));
                }

                // Get the Public Key value after the `:` colon in the string `Public Key:`
                publicKey = lines.get(i).split(":")[1].strip();

                if (publicKey == null || publicKey.length() == 0) {
                    throw new BlockParseException("Make sure you write the public key after the \"Public Key:\" string.\n" +
                            "For example \"Public Key: MFkw...\"");
                }

                // Check if the Public Key starts with `MF` and has a length of 120 or 124 characters
                if (!publicKey.startsWith("MF") || (publicKey.length() != 120 && publicKey.length() != 124)) {
                    throw new BlockParseException("The Public Key should be in the PKIX, ASN.1 DER form and have a "
                            + "length of 120 or 124 characters.\n" +
                            "Your Public Key: " + publicKey + "\n" +
                            "Your Public Key length: " + publicKey.length());
                }

                i += 1;  // Get the line — `Block was generating for ...`
                txCounter += 1; // Increment the transaction counter
            }

            if (!lines.get(i).toLowerCase().contains("block") || !lines.get(i).toLowerCase().contains("generating")) {
                throw new BlockParseException("After the line with the Public Key of the message, " +
                        "the next line should state how long the block was generating for! " +
                        "(Use the example's format)" +

                        "\n" + "Your program instead printed in Block " +
                        block.id + " an unexpected line: " + lines.get(i));
            }

            i += 1;  // Get the line — `N ...`

            if (i != lines.size() - 1) {
                throw new BlockParseException("Your program printed in Block " + block.id +
                        " after the line: \"N was increased/decreased/stays the same\"\n" +
                        "an additional and unexpected line: " + lines.get(lines.size() - 1));
            }

            if (!lines.get(i).toUpperCase().startsWith("N ")) {
                throw new BlockParseException("After the line `Block was generating for ...` " +
                        "the next line should state what happened to N in the format given." +

                        "\n" + "Your program instead printed in Block " +
                        block.id + " an unexpected line: " + lines.get(i));
            }

            if (lines.get(i).toLowerCase().contains("increase")) {
                N += 1;
            } else if (lines.get(i).toLowerCase().contains("decrease")) {
                N -= 1;
                if (N < 0) {
                    throw new BlockParseException("N was decreased below zero!");
                }
            } else if (!lines.get(i).toLowerCase().contains("same")) {
                throw new BlockParseException("The last line of every Block "
                        + "must state N increased, decreased, or stayed the same.");
            }
        }
        return block;
    }


    static List<Block> parseBlocks(String output) throws BlockParseException {
        minerIds = new ArrayList<String>();
        N = 0;

        String[] strBlocks = output.split("\n\n");

        List<String> skipWords = Arrays.asList("enter how many transactions", "from user", "to user", "amount");
        List<Block> blocks = new ArrayList<>();

        for (String str_block : strBlocks) {
            // Skip the transaction input lines and append the blocks:
            if (!skipWords.stream().anyMatch(str_block.toLowerCase()::startsWith)) {
                Block block = Block.parseBlock(str_block.strip());
                if (block != null) {
                    blocks.add(block);
                }
            }
        }

        String firstMiner = Block.minerIds.get(0);

        List<String> minerIds = Block.minerIds.stream().filter(s -> !s.equals(firstMiner)).collect(Collectors.toList());
        if (minerIds.isEmpty()) {
            throw new BlockParseException("All blocks are mined by a single miner!");
        }

        return blocks;
    }
}

class TransactionParseException extends Exception {
    TransactionParseException(String msg) {
        super(msg);
    }
}

class TransactionOutputs {
    public static List<List<String>> txTestOutputs = Arrays.asList(
            // Test case 1
            // Block 2
            Arrays.asList("Nick remaining balance: 70 VC", "Alice new balance: 130 VC"),
            Arrays.asList("User Nick doesn't have enough VC to send", "Nick current balance: 70 VC"),

            // Block 3
            Arrays.asList("Alice remaining balance: 50 VC", "Nick new balance: 150 VC"),
            Arrays.asList("You can't send VC from one user to the same user"),

            // Block 4
            Arrays.asList("User Alice doesn't have enough VC to send", "Alice current balance: 50 VC"),
            Arrays.asList("HyperGlam remaining balance: 70 VC", "Alice new balance: 80 VC"),
            Arrays.asList("Alice remaining balance: 78 VC", "KrustyBurger new balance: 102 VC"),

            // Block 5
            Arrays.asList("IroncladConstruction remaining balance: 70 VC", "Nick new balance: 180 VC"),
            Arrays.asList("Nick remaining balance: 5 VC", "FerrariShop new balance: 275 VC"),
            Arrays.asList("Nick remaining balance: 1 VC", "FuelStation new balance: 104 VC"),

            // Test case 2
            // Block 2
            Arrays.asList("Rick remaining balance: 90 VC", "Morty new balance: 110 VC"),
            Arrays.asList("User Rick doesn't have enough VC to send", "Rick current balance: 90 VC"),
            Arrays.asList("Rick remaining balance: 70 VC", "Beth new balance: 120 VC"),
            Arrays.asList("Rick remaining balance: 40 VC", "Summer new balance: 130 VC"),
            Arrays.asList("Jerry remaining balance: 0 VC", "Rick new balance: 140 VC"),

            // Block 3
            Arrays.asList("Morty remaining balance: 108 VC", "ShoneysRestaurant new balance: 102 VC"),
            Arrays.asList("Summer remaining balance: 120 VC", "SephoraShop new balance: 110 VC"),
            Arrays.asList("Beth remaining balance: 100 VC", "GucciShop new balance: 120 VC"),
            Arrays.asList("User Jerry doesn't have enough VC to send", "Jerry current balance: 0 VC"),

            // Block 4
            Arrays.asList("PlumbusCompany remaining balance: 99 VC", "Jerry new balance: 1 VC"),
            Arrays.asList("User Jerry doesn't have enough VC to send", "Jerry current balance: 1 VC"),
            Arrays.asList("You can't send VC from one user to the same user"),

            // Block 5
            Arrays.asList("VetClinic remaining balance: 50 VC", "Beth new balance: 150 VC"),
            Arrays.asList("Beth remaining balance: 149 VC", "Jerry new balance: 2 VC"),
            Arrays.asList("Beth remaining balance: 130 VC", "WineShop new balance: 119 VC")
    );
}


class Transaction {
    static int id;
    static int blockId;
    // String fromUser;
    // String toUser;
    // int amount;

    static Transaction parseTransaction(String strTransaction) throws TransactionParseException {
        if (strTransaction.length() == 0) {
            return null;
        }

        Transaction transaction = new Transaction();

        List<String> txLines = strTransaction
                .lines()
                .map(String::strip)
                .filter(e -> e.length() > 0)
                .collect(Collectors.toList());

        // Index of the first line of the transaction prompt, it can either be one of two cases:
        int i = 0;
        // - `Enter how many transactions you want to perform:`
        // - `From user:`

        // Check if the first line is `Enter how many transactions you want to perform:`
        if (txLines.get(i).toLowerCase().startsWith("enter how many transactions")) {
            i += 1; // - Index of the line `From user:`
            if (!txLines.get(i).toLowerCase().startsWith("from user")) {
                throw new TransactionParseException(
                        "After the prompt \"Enter how many transactions you want to perform:\" "
                                + "the next prompt should be \"From user:\"");
            }

            i += 1; // - Index of the line `To user:`
            if (!txLines.get(i).toLowerCase().startsWith("to user")) {
                throw new TransactionParseException(
                        "After the prompt \"From user:\" "
                                + "the next prompt should be \"To user:\"");
            }

            i += 1; // - Index of the line `Amount:`
            if (!txLines.get(i).toLowerCase().startsWith("vc amount")) {
                throw new TransactionParseException(
                        "After the prompt \"To user:\" "
                                + "the next prompt should be \"Amount:\"");
            }

            i += 1; // - Index of the line `Transaction is valid/invalid`
            if (!txLines.get(i).toLowerCase().startsWith("transaction is")) {
                throw new TransactionParseException(
                        "After the prompt \"Amount:\" the program should output "
                                + "\"Transaction is valid\" for valid transactions\n"
                                + "or \"Transaction is not valid\" for invalid transactions");
            }
        }

        // Check if the first line is `From user:`
        if (txLines.get(i).toLowerCase().startsWith("from user")) {
            i += 1; // - Index of the line `To user:`
            if (!txLines.get(i).toLowerCase().startsWith("to user")) {
                throw new TransactionParseException(
                        "After the prompt \"From user:\" "
                                + "the next prompt should be \"To user:\"");
            }

            i += 1; // - Index of the line `Amount:`
            if (!txLines.get(i).toLowerCase().startsWith("vc amount")) {
                throw new TransactionParseException(
                        "After the prompt \"To user:\" "
                                + "the next prompt should be \"Amount:\"");
            }

            i += 1; // - Index of the line `Transaction is valid/invalid`
            if (!txLines.get(i).toLowerCase().startsWith("transaction is")) {
                throw new TransactionParseException(
                        "After the prompt \"Amount:\" the program should output "
                                + "\"Transaction is valid\" for valid transactions\n"
                                + "or \"Transaction is not valid\" for invalid transactions");
            }
        }

        // Now we move to check valid and not valid/invalid transactions
        // Check the case of valid transaction
        if (txLines.get(i).toLowerCase().startsWith("transaction is valid")) {
            List<List<String>> txTestOutputs = TransactionOutputs.txTestOutputs;
            String[] lastLines = txLines.subList(i + 1, txLines.size()).toArray(new String[0]);

            for (int j = 0; j < txTestOutputs.get(Transaction.id).size(); j++) {
                if (!txTestOutputs.get(Transaction.id).get(j).replace(":", "").equalsIgnoreCase(lastLines[j].replace(":", ""))) {
                    throw new TransactionParseException(
                            "Below Block " + Transaction.blockId +
                                    " After the line \"Transaction is valid\" the program should print "
                                    + "\"" + txTestOutputs.get(Transaction.id).get(j) + "\"\n" +
                                    "Your program instead printed: " + "\"" + lastLines[j] + "\"");
                }
            }
        }

        // Check the case of invalid transaction
        if (txLines.get(i).toLowerCase().startsWith("transaction is not valid")) {
            List<List<String>> txTestOutputs = TransactionOutputs.txTestOutputs;
            String[] lastLines = txLines.subList(i + 1, txLines.size()).toArray(new String[0]);

            for (int j = 0; j < txTestOutputs.get(Transaction.id).size(); j++) {
                if (!txTestOutputs.get(Transaction.id).get(j).replace(":", "").equalsIgnoreCase(lastLines[j].replace(":", ""))) {
                    throw new TransactionParseException(
                            "Below Block " + Transaction.blockId +
                                    " After the line \"Transaction is not valid\" the program should print "
                                    + "\"" + txTestOutputs.get(Transaction.id).get(j) + "\"\n" +
                                    "Your program instead printed: " + "\"" + lastLines[j] + "\"");
                }
            }
        }
        return transaction;
    }

    public static void parseTransactions(String output) throws TransactionParseException {
        String[] strTransactions = output.split("\n\n");
        List<Transaction> transactions = new ArrayList<Transaction>();

        for (String strTransaction : strTransactions) {
            if (strTransaction.toLowerCase().startsWith("genesis block")) {
                continue;
            }

            if (strTransaction.toLowerCase().startsWith("block")) {
                Transaction.blockId = Integer.parseInt(strTransaction.split("\n")[2].split(":")[1].trim());
                continue;
            }

            if (strTransaction.toLowerCase().startsWith("enter how many transactions") || strTransaction.toLowerCase().startsWith("from user")) {
                Transaction tr = Transaction.parseTransaction(strTransaction);
                if (tr != null) {
                    Transaction.id += 1;
                    transactions.add(tr);
                }
            } else {
                throw new TransactionParseException(
                        "In every Block except for the fifth Block, after the line that states what happened to `N`\n"
                                + "\"N was increased/decreased/stays the same\" your program should prompt the user: "
                                + "\"Enter how many transactions you want to perform:\"\n"
                                + "Your program instead printed: " + "\"" + strTransaction.split("\n")[0] + "\"");

            }
        }
    }
}

class Clue {
    String zeros;

    Clue(int n) {
        zeros = "0".repeat(n);
    }
}


public class BlockchainTest extends StageTest<Clue> {

    List<String> previousOutputs = new ArrayList<>();

    String testTxInput1 = """
            2
            Nick
            Alice
            30
            Nick
            FerrariShop
            71
            2
            Alice
            Nick
            80
            Alice
            Alice
            50
            3
            Alice
            SephoraShop
            60
            HyperGlam
            Alice
            30
            Alice
            KrustyBurger
            2
            3
            IroncladConstruction
            Nick
            30
            Nick
            FerrariShop
            175
            Nick
            FuelStation
            4
            """;

    String testTxInput2 = """
            5
            Rick
            Morty
            10
            Rick
            Beth
            100
            Rick
            Beth
            20
            Rick
            Summer
            30
            Jerry
            Rick
            100
            4
            Morty
            ShoneysRestaurant
            2
            Summer
            SephoraShop
            10
            Beth
            GucciShop
            20
            Jerry
            Beth
            1
            3
            PlumbusCompany
            Jerry
            1
            Jerry
            Beth
            5
            Jerry
            Jerry
            10
            3
            VetClinic
            Beth
            50
            Beth
            Jerry
            1
            Beth
            WineShop
            19
            """;

    @Override
    public List<TestCase<Clue>> generate() {
        return List.of(
                new TestCase<Clue>().setInput(testTxInput1).setAttach(new Clue(0)),
                new TestCase<Clue>().setInput(testTxInput2).setAttach(new Clue(0))
        );
    }

    @Override
    public CheckResult check(String reply, Clue clue) {

        if (previousOutputs.contains(reply)) {
            return new CheckResult(false,
                    "You already printed this text in the previous tests");
        }

        previousOutputs.add(reply);

        List<Block> blocks;
        try {
            blocks = Block.parseBlocks(reply);
        } catch (BlockParseException ex) {
            return new CheckResult(false, ex.getMessage());
        } catch (Exception ex) {
            // return CheckResult.wrong("Something went wrong while parsing the block data:\n" + ex.getMessage());
            return CheckResult.wrong(
                    "Something went wrong while parsing the Block data:\n" +
                            ex.getMessage() + "\n" +
                            "Please make sure that your program's output exactly matches the Example Output format.\n" +
                            "An empty new line `\\n` must separate each prompt to enter a message, as well as each Block.\n"
            );
        }

        // NEW -- Parse the transaction prompt/input after the block details:
        try {
            Transaction.parseTransactions(reply);
        } catch (TransactionParseException ex) {
            return new CheckResult(false, ex.getMessage());
        } catch (Exception ex) {
            // return CheckResult.wrong("Something went wrong while parsing the transaction data:\n" + ex.getMessage());
            return CheckResult.wrong(
                    "Something went wrong while parsing the prompt to enter each transaction:\n" +
                            ex.getMessage() + "\n" +
                            "Please make sure that your program's output exactly matches the Example Output format.\n" +
                            "An empty new line `\\n` must separate each prompt to enter each transaction, as well as each Block.\n"
            );
        }

        if (blocks.size() != 5) {
            return new CheckResult(false,
                    "In this stage you should output 5 blocks, found " + blocks.size());
        }

        for (int i = 1; i < blocks.size(); i++) {
            Block curr = blocks.get(i - 1);
            Block next = blocks.get(i);

            if (curr.id + 1 != next.id) {
                return new CheckResult(false,
                        "Id's of blocks should increase by 1");
            }

            if (next.timestamp < curr.timestamp) {
                System.out.println(next.timestamp);
                System.out.println(curr.timestamp);
                return new CheckResult(false,
                        "Timestamp's of blocks should increase");
            }

            if (!next.prevHash.equals(curr.hash)) {
                return new CheckResult(false, "Two hashes aren't equal, " +
                        "but should");
            }
        }

        return CheckResult.correct();
    }
}