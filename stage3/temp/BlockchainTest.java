import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.hyperskill.hstest.testcase.TestCase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    int blockGeneratingTime;

    static int prevBlockId = 0;

    static long prevTimestamp = 0;

    static int blockCounter = 0;

    static ArrayList<String> minerIds;
    static int N;

    static Block parseBlock(String strBlock) throws BlockParseException, NoSuchAlgorithmException {
        if (strBlock.length() == 0) {
            return null;
        }

        if (!(strBlock.toLowerCase().contains("block")
                && strBlock.toLowerCase().contains("timestamp"))) {

            return null;
        }

        Block block = new Block();

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

        if (lines.size() < 10) {
            throw new BlockParseException("Every Block should " +
                    "contain 11 lines of data");
        }

        // NEW Check if the current block id is not 1 greater than the previous block id
        if (block.id <= 0 && !lines.get(0).toLowerCase().startsWith("genesis block")) {
            throw new BlockParseException(
                    "Each subsequent Block Id should increment by 1. Found Block Id: " + block.id + ", expected Block Id: " + (prevBlockId + 1) +
                            "\nYour program printed as the third line in Block " + blockCounter + ": " + "\"" + lines.get(2) + "\""
            );
        }

        if (block.id != prevBlockId + 1 && lines.get(0).toLowerCase().startsWith("genesis block")) {
            blockCounter += 1;
//            throw new BlockParseException(
//                    "Each subsequent Block Id should increment by 1. Found Block Id: " + block.id + ", expected Block Id: " + (prevBlockId + 1) +
//                            "\nYour program printed as the second line in Block " + blockCounter + ": " + "\"" + lines.get(1) + "\""
//            );
            throw new BlockParseException(
                    "You have printed the Genesis Block more than once. " +
                            "The Genesis Block should be printed only once at the beginning of the blockchain."
            );
        }

        if (block.id != prevBlockId + 1 && !lines.get(0).toLowerCase().startsWith("genesis block")) {
            blockCounter += 1;
            throw new BlockParseException(
                    "Each subsequent Block Id should increment by 1. Found Block Id: " + block.id + ", expected Block Id: " + (prevBlockId + 1) +
                            "\nYour program printed as the third line in Block " + blockCounter + ": " + "\"" + lines.get(2) + "\""
            );
        }

        prevBlockId = block.id; // Update the previous block id after the check

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

            // NEW - Check that the hash of the previous block is "0" for the first/genesis block.
            if (!prevHash.equals("0")) {
                throw new BlockParseException("The \"Hash of the previous block\" of the Genesis Block must be \"0\".\nYour program instead printed: \"" + lines.get(5) + "\"");
            }
            // END - Check that the hash of the previous block is "0" for the first/genesis block.

            // NEW - Tests to check the hash of the Genesis Block
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // String blockData = block.id + timestamp + prevHash;
            String blockData = String.valueOf(block.id) + String.valueOf(block.timestamp) + String.valueOf(block.magic) + prevHash;

            byte[] hashBytes = digest.digest(blockData.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            String calculatedHash = sb.toString();

            if (!calculatedHash.equals(hash)) {
                throw new BlockParseException("The hash of Block " + block.id + " is incorrect.\nYour program printed: \"" + lines.get(7) + "\"\n"
                        + "Expected hash of the block: " + calculatedHash + "\n" +
                        "Make sure you are only using the Block's id, timestamp and the hash of the previous block to calculate the hash of the block.");
            }
            // END - Tests to check the hash of the block


//            if (!(prevHash.length() == 64 || prevHash.equals("0")) || hash.length() != 64) {
//                throw new BlockParseException("Hash length should be equal to 64 except \"0\"");
//            }

            if (hash.equals(prevHash)) {
                throw new BlockParseException("The current hash and the previous hash in a block should be different.");
            }

            if (!hash.startsWith("0".repeat(N))) {
                throw new BlockParseException("N is " + N + " but hash, " + hash + ", did not start with the correct number of zeros.");
            }
            block.hash = hash;
            block.prevHash = prevHash;

            if (!(lines.get(8).toLowerCase().contains("block") || lines.get(10).toLowerCase().contains("generating"))) {
                throw new BlockParseException("9-th line of the Genesis Block " +
                        "should say how long the block was generating for! "
                        + "(Use the example's format)"

                        + "\n" + "Your program instead printed in Block " +
                        block.id + " an unexpected line: " + lines.get(10));
            }

            // NEW - Tests to check block generation time
            String blockGeneratingTimeStr = lines.get(8).split(" ")[4];
            isNumeric = blockGeneratingTimeStr.chars().allMatch(Character::isDigit);

            if (!isNumeric) {
                throw new BlockParseException("Block generating time should be an integer number in seconds");
            }

            int blockGeneratingTime = Integer.parseInt(blockGeneratingTimeStr);
            block.blockGeneratingTime = blockGeneratingTime;

            int maxGenesisBlockGeneratingTime = 10;

            if (blockGeneratingTime < 0 || blockGeneratingTime > maxGenesisBlockGeneratingTime) {
                throw new BlockParseException(
                        "Block generating time for the Genesis block should be between 0 and " + maxGenesisBlockGeneratingTime + " seconds. " +
                                "Your program took " + blockGeneratingTime + " seconds to generate the Genesis block." + "\n" +
                                "You need to make the algorithm that generates the hash more efficient."
                );
            }

            prevTimestamp = block.timestamp; // Update the previous timestamp after the check

            // END - Tests to check block generation time


            if (!lines.get(9).toUpperCase().startsWith("N ")) {
                throw new BlockParseException("10-th line of the Genesis Block " +
                        "should be state what happened to N in the format given."

                        + "\n" + "Your program instead printed in Block " +
                        block.id + " an unexpected line: " + lines.get(9));
            }

            if (lines.get(9).toLowerCase().contains("increase")) {
                N += 1;
            } else if (lines.get(9).toLowerCase().contains("decrease")) {
                N -= 1;
                if (N < 0) {
                    throw new BlockParseException("N was decreased below zero!");
                }
            } else if (!lines.get(9).toLowerCase().contains("same")) {
                throw new BlockParseException("The last line of every block" +
                        "must state N increased, decreased, or stayed the same.");
            }

            if (9 != lines.size() - 1) {
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

//            if (!(prevHash.length() == 64 || prevHash.equals("0")) || hash.length() != 64) {
//                throw new BlockParseException("Hash length should be equal to 64 except \"0\"");
//            }

            // NEW - Tests to check the hash of the block
            if (!(prevHash.length() == 64) || !(hash.length() == 64)) {
                throw new BlockParseException("Every subsequent Block's \"Hash of the previous block\" length should be a SHA-256 hash, which is 64 characters long." +
                        "\nYour program instead printed: \"" + lines.get(8) + "\"");
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // String blockData = block.id + timestamp + prevHash;
            String blockData = String.valueOf(block.id) + String.valueOf(block.timestamp) + String.valueOf(block.magic) + prevHash;

            byte[] hashBytes = digest.digest(blockData.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            String calculatedHash = sb.toString();

            if (!calculatedHash.equals(hash)) {
                throw new BlockParseException("The hash of Block " + block.id + " is incorrect.\nYour program printed: \"" + lines.get(8) + "\"\n"
                        + "Expected hash of the block: " + calculatedHash + "\n" +
                        "Make sure you are only using the Block's id, timestamp, magic number and the hash of the previous block to calculate the hash of the block.");
            }
            // END - Tests to check the hash of the block

            if (hash.equals(prevHash)) {
                throw new BlockParseException("The current hash and the previous hash in a block should be different.");
            }

            if (!hash.startsWith("0".repeat(N))) {
                throw new BlockParseException("N is " + N + " but hash, " + hash + ", did not start with the correct number of zeros.");
            }
            block.hash = hash;
            block.prevHash = prevHash;

            if (!lines.get(9).toLowerCase().contains("block") && !lines.get(9).toLowerCase().contains("generating")) {
                throw new BlockParseException("10-th line of every subsequent Block " +
                        "should say how long the block was generating for! "
                        + "(Use the example's format)"

                        + "\n" + "Your program instead printed in Block " +
                        block.id + " an unexpected line: " + lines.get(9));
            }

            // NEW - Tests to check the time it took to generate the block
            String blockGeneratingTimeStr = lines.get(9).split(" ")[4];
            isNumeric = blockGeneratingTimeStr.chars().allMatch(Character::isDigit);

            if (!isNumeric) {
                throw new BlockParseException("Block generating time should be an integer number in seconds");
            }

            int blockGeneratingTime = Integer.parseInt(blockGeneratingTimeStr);
            block.blockGeneratingTime = blockGeneratingTime;

            long timeDifferenceMillis = block.timestamp - prevTimestamp;
            // double timeDifferenceSeconds = TimeUnit.NANOSECONDS.toSeconds(timeDifferenceMillis);
            long timeDifferenceSeconds = timeDifferenceMillis / 1_000_000_000;

            if (blockGeneratingTime < 0 || blockGeneratingTime != timeDifferenceSeconds) {
                throw new BlockParseException(
                        "Block generating time for Block " + block.id + " is incorrect.\nYour program printed: \"" + lines.get(8) + "\". "
                                + "Actual block generating time: " + timeDifferenceSeconds + " seconds."
                );
            }

            prevTimestamp = block.timestamp; // Update the previous timestamp after the check

            // END - Tests to check the time it took to generate the block


            if (!lines.get(10).toUpperCase().startsWith("N ")) {
                throw new BlockParseException("11-th line of every subsequent Block " +
                        "should be state what happened to N in the format given."

                        + "\n" + "Your program instead printed in Block " +
                        block.id + " an unexpected line: " + lines.get(9));
            }

            if (lines.get(10).toLowerCase().contains("increase")) {
                N += 1;
            } else if (lines.get(10).toLowerCase().contains("decrease")) {
                N -= 1;
                if (N < 0) {
                    throw new BlockParseException("N was decreased below zero!");
                }
            } else if (!lines.get(10).toLowerCase().contains("same")) {
                throw new BlockParseException("The last line of every block" +
                        "must state N increased, decreased, or stayed the same.");
            }

            if (10 != lines.size() - 1) {
                throw new BlockParseException("Your program printed in Block " + block.id +
                        " after the line: \"N was increased/decreased/stays the same\"\n" +
                        "an additional and unexpected line: " + lines.get(lines.size() - 1));
            }
        }

        // Reset the block counter after the 5th block:
        if (blockCounter < 6) {
            blockCounter += 1;
        } else {
            blockCounter = 0;
        }

        return block;
    }


    static List<Block> parseBlocks(String output) throws BlockParseException, NoSuchAlgorithmException {
        minerIds = new ArrayList<String>();
        N = 0;

        String[] strBlocks = output.split("\n\n");

        List<Block> blocks = new ArrayList<>();

        for (String strBlock : strBlocks) {
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

class Clue {
    String zeros;

    Clue(int n) {
        zeros = "0".repeat(n);
    }
}


public class BlockchainTest extends StageTest<Clue> {

    List<String> previousOutputs = new ArrayList<>();

    @Override
    public List<TestCase<Clue>> generate() {
        return List.of(
                new TestCase<>(),
                new TestCase<>()
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
            // return CheckResult.wrong("");
            return CheckResult.wrong(
                    "Something went wrong while parsing the Block data:\n" +
                            ex.getMessage() + "\n" +
                            "Please make sure that your program's output exactly matches the Example Output format.\n" +
                            "An empty new line `\\n` must separate each Block.\n"
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
