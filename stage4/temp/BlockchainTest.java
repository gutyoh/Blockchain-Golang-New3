import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.hyperskill.hstest.testcase.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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

    int generationTime;

    static int prevBlockId = 0;

    static long prevTimestamp = 0;

    static int blockCounter = 1;

    static ArrayList<String> minerIds;
    static int N;

    String[] blockDataMessage = {
            // Test case #1
            // Block 2
            "Tom: Hey, I'm first",

            // Block 3
            "Alice: It's not fair! You always will be first because it is your blockchain!",

            // Block 4
            "Alice: Anyway, thank you for this amazing chat",

            // Block 5
            "Tom: You're welcome, Alice :)",

            // ----------------------------------------------

            // Test case #2
            // Block 2
            "Tom: Hey, I'm first once again!",

            // Block 3
            "Nick: Hey Tom, nice Blockchain chat you created!",

            // Block 4
            "Tom: Thanks, Nick! It was a lot of fun to create it!",

            // Block 5
            "Tom: Anyways, I have to leave for a meeting now. Enjoy the blockchain chat. Bye!",
    };


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

//        if (lines.size() < 12) {
//            throw new BlockParseException("Every block should contain at least 12 lines of data");
//        }

        if (lines.size() < 12) {
            if (block.id == 1) {
                throw new BlockParseException("The Genesis Block should " +
                        "contain 12 lines of data and no empty new lines within its Block data.\n" +
                        "Ensure you print each Block in the same format as the example output."
                );
            } else {
                throw new BlockParseException("Every subsequent Block should " +
                        "contain at least 12 lines of data and no empty new lines within its Block data.\n" +
                        "Ensure you print each Block in the same format as the example output."
                );
            }
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

            if (!lines.get(9).toLowerCase().contains("no messages")) {
                throw new BlockParseException("10-th line of the Genesis Block " +
                        "should contain \"no messages\"");
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

            int i = 10; // Get the line with the first chat message after `Block data:`

            if (!Arrays.asList(block.blockDataMessage).contains(lines.get(i))) {
                throw new BlockParseException("In Block" + block.id + " the chat message within the Block data "
                        + "should be: " + block.blockDataMessage[0] +

                        "\n" + "Your program instead printed in Block" + block.id +
                        " an unexpected line: " + lines.get(i));
            }

            // Skip all the chat messages (Block data) until we reach the line `Block was generating for ...`
            while (!lines.get(i).toLowerCase().startsWith("block was generating")) {
                i++;
            }

            // After the loop, we should reach the line `Block was generating for ...`
            if (!lines.get(i).toLowerCase().contains("block") && !lines.get(i).toLowerCase().contains("generating")) {
                throw new BlockParseException("After the line with the Public Key of the message, " +
                        "the next line should state how long the block was generating for! " +
                        "(Use the example's format)" +

                        "\n" + "Your program instead printed in Block" +
                        block.id + " an unexpected line: " + lines.get(i));
            }

            i++;  // Get the line — `N ...`

            if (i != lines.size() - 1) {
                throw new BlockParseException("Your program printed in Block" + block.id +
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
                throw new BlockParseException("The last line of every block" +
                        "must state N increased, decreased, or stayed the same.");
            }
        }

        return block;
    }


    static List<Block> parseBlocks(String output) throws BlockParseException {
        minerIds = new ArrayList<String>();
        N = 0;

        String[] strBlocks = output.split("\n\n");
        List<Block> blocks = new ArrayList<>();

        for (String strBlock : strBlocks) {
            if (strBlock.toLowerCase().startsWith("enter a single message")) {
                continue;
            }

            Block block = parseBlock(strBlock.strip());
            if (block != null) {
                blocks.add(block);
            }
        }

        String firstMiner = minerIds.get(0);
        minerIds.removeIf(s -> Objects.equals(s, firstMiner));
        if (minerIds.size() == 0) {
            throw new BlockParseException("All blocks are mined by a single miner!");
        }

        return blocks;
    }
}

class MessageParseException extends Exception {
    MessageParseException(String msg) {
        super(msg);
    }
}

class Message {

    public static void parseMessagePrompt(String output) throws MessageParseException {
        String[] strMessage = output.split("\n\n");

        // iterate over the odd lines in the strMessage list and check if they start with "enter a single message"
        for (int i = 1; i < strMessage.length; i += 2) {
            if (!strMessage[i].toLowerCase().startsWith("enter a single message")) {
                throw new MessageParseException(
                        "In every Block except for the fifth Block, after the line that states what happened to `N`\n"
                                + "\"N was increased/decreased/stays the same\" your program should prompt the user: "
                                + "\"Enter a single message to send to the Blockchain:\"\n"
                                + "Your program instead printed: " + "\"" + strMessage[i] + "\"");
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

    static String testMessagesInput1 = """
            Tom: Hey, I'm first
            Alice: It's not fair! You always will be first because it is your blockchain!
            Alice: Anyway, thank you for this amazing chat
            Tom: You're welcome, Alice :)
            """;

    static String testMessagesInput2 = """
            Tom: Hey, I'm first once again!
            Nick: Hey Tom, nice Blockchain chat you created!
            Tom: Thanks, Nick! It was a lot of fun to create it!
            Tom: Anyways, I have to leave for a meeting now. Enjoy the blockchain chat. Bye!
            """;

    @Override
    public List<TestCase<Clue>> generate() {
        return List.of(
                new TestCase<Clue>().setInput(testMessagesInput1).setAttach(new Clue(0)),
                new TestCase<Clue>().setInput(testMessagesInput2).setAttach(new Clue(0))
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

        // # NEW -- Parse the "enter single message" line
        try {
            Message.parseMessagePrompt(reply);
        } catch (MessageParseException ex) {
            return new CheckResult(false, ex.getMessage());
        } catch (Exception ex) {
            // return CheckResult.wrong("Something went wrong while parsing the enter message prompt:\n" + ex.getMessage());
            return CheckResult.wrong(
                    "Something went wrong while parsing the prompt to enter a message:\n" +
                            ex.getMessage() + "\n" +
                            "Please make sure that your program's output exactly matches the Example Output format.\n" +
                            "An empty new line `\\n` must separate each prompt to enter a message, as well as each Block.\n"
            );
        }

        if (blocks.size() != 5) {
            return new CheckResult(false,
                    "You should output 5 blocks, found " + blocks.size());
        }

        for (int i = 1; i < blocks.size(); i++) {
            Block curr = blocks.get(i - 1);
            Block next = blocks.get(i);

            if (curr.id + 1 != next.id) {
                return new CheckResult(false,
                        "Id's of blocks should increase by 1");
            }

            if (next.timestamp < curr.timestamp) {
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