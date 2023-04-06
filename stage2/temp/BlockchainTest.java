import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.hyperskill.hstest.testcase.TestCase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

    static int blockCounter = 1;

    static Block parseBlock(String strBlock) throws BlockParseException, NoSuchAlgorithmException {
        if (strBlock.length() == 0) {
            return null;
        }

        if (!(strBlock.toLowerCase().contains("block")
                && strBlock.toLowerCase().contains("timestamp"))) {

            return null;
        }

        Block block = new Block();

        List<String> lines = strBlock
                .lines()
                .map(String::strip)
                .filter(e -> e.length() > 0)
                .collect(Collectors.toList());

        if (lines.size() != 9) {
            throw new BlockParseException("Every Block should " +
                    "contain 9 lines of data");
        }

        // 1. Validate the block headers:
        validateBlockHeader(block, lines);

        // 2. Validate the block id:

        validateBlockId(block, lines);

        // 3. Validate the timestamp:

        validateTimestamp(block, lines);

        // 4. Validate the previous hash and the current hash:

        validatePrevHashAndHash(block, lines);

        // 5. Update the block counter:

        updateBlockCounter();

        if (!lines.get(0).toLowerCase().startsWith("block") && !lines.get(0).toLowerCase().startsWith("genesis block")) {
            throw new BlockParseException("The first line of the first block in the blockchain should be \"Genesis Block:\" and every subsequent Block's first line should be \"Block:\"" +
                    "\nYour program instead printed as the first line in Block " + block.id + ": " + "\"" + lines.get(0) + "\"");
        }

        if (!lines.get(1).toLowerCase().startsWith("id:")) {
            throw new BlockParseException("Second line of every Block " +
                    "should start with \"Id:\"");
        }

        String id = lines.get(1).split(":")[1]
                .strip().replace("-", "");
        boolean isNumeric = id.chars().allMatch(Character::isDigit);

        if (!isNumeric) {
            throw new BlockParseException("Id should be a number");
        }

        block.id = Integer.parseInt(id);

        // NEW Check if the current block id is not 1 greater than the previous block id
        if (block.id <= 0 && !lines.get(0).toLowerCase().startsWith("genesis block")) {
            throw new BlockParseException(
                    "Each subsequent Block Id should increment by 1. Found Block Id: " + block.id + ", expected Block Id: " + (prevBlockId + 1) +
                            "\nYour program printed as the second line in Block " + blockCounter + ": " + "\"" + lines.get(1) + "\""
            );
        }

        // NEW Check that the genesis block has Id = 1
        if (block.id != 1 && lines.get(0).toLowerCase().startsWith("genesis block")) {
            throw new BlockParseException(
                    "The Genesis Block must have Id: 1\n" +
                            "Your program printed as the second line in the Genesis Block: " + "\"" + lines.get(1) + "\""
            );
        }

        if (block.id != prevBlockId + 1 && lines.get(0).toLowerCase().startsWith("genesis block")) {
            throw new BlockParseException(
                    "You have printed the Genesis Block more than once. " +
                            "The Genesis Block should be printed only once at the beginning of the blockchain."
            );
        }

        if (block.id != prevBlockId + 1 && !lines.get(0).toLowerCase().startsWith("genesis block")) {
            blockCounter += 1;
            throw new BlockParseException(
                    "Each subsequent Block Id should increment by 1. Found Block Id: " + block.id + ", expected Block Id: " + (prevBlockId + 1) +
                            "\nYour program printed as the second line in Block " + blockCounter + ": " + "\"" + lines.get(1) + "\""
            );
        }

        prevBlockId = block.id; // Update the previous block id after the check

        if (!lines.get(2).toLowerCase().startsWith("timestamp:")) {
            throw new BlockParseException("Third line of every Block " +
                    "should start with \"Timestamp:\"");
        }

        String timestamp = lines.get(2).split(":")[1]
                .strip().replace("-", "");
        isNumeric = timestamp.chars().allMatch(Character::isDigit);

        if (!isNumeric) {
            throw new BlockParseException("Timestamp should be a number");
        }

        block.timestamp = Long.parseLong(timestamp);

        if (!lines.get(3).toLowerCase().startsWith("magic number:")) {
            throw new BlockParseException("4-th line of every Block " +
                    "should start with \"Magic number:\"");
        }

        String magic = lines.get(3).split(":")[1]
                .strip().replace("-", "");
        isNumeric = magic.chars().allMatch(Character::isDigit);

        if (!isNumeric) {
            throw new BlockParseException("Timestamp should be a number");
        }

        block.magic = Long.parseLong(magic);

        if (!lines.get(4).equalsIgnoreCase("hash of the previous block:")) {
            throw new BlockParseException("5-th line of every Block " +
                    "should be \"Hash of the previous block:\"");
        }

        if (!lines.get(6).equalsIgnoreCase("hash of the block:")) {
            throw new BlockParseException("7-th line of every Block " +
                    "should be \"Hash of the block:\"");
        }

        String prevHash = lines.get(5).strip();
        String hash = lines.get(7).strip();

        // NEW Check that the hash of the previous block is "0" for the first/genesis block.
        if (block.id == 1 && blockCounter == 1) {
            if (!prevHash.equals("0")) {
                throw new BlockParseException("The \"Hash of the previous block\" of the Genesis Block must be \"0\".\nYour program instead printed: \"" + lines.get(5) + "\"");
            }
        }

        // Use the block's id, timestamp and previous hash to calculate the hash of the block.
        if (block.id  > 1 && blockCounter > 1) {

            if (!(prevHash.length() == 64) || !(hash.length() == 64)) {
                throw new BlockParseException("Every subsequent Block's \"Hash of the previous block\" length should be a SHA-256 hash, which is 64 characters long." +
                        "\nYour program instead printed: \"" + lines.get(5) + "\"");
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
                throw new BlockParseException("The hash of Block " + block.id + " is incorrect.\nYour program printed: \"" + lines.get(7) + "\"\n"
                        + "Expected Hash of the block: " + calculatedHash + "\n" +
                        "Make sure you are only using the Block's id, timestamp, magic number and the Hash of the previous block to calculate the Hash of the block.");
            }
        }

        if (hash.equals(prevHash)) {
            throw new BlockParseException("The current hash and the previous hash in a block should be different.");
        }

        block.hash = hash;
        block.prevHash = prevHash;


        // Check the First/Genesis block:
        if (block.id == 1) {
            if (!lines.get(0).toLowerCase().contains("genesis block")) {
                throw new BlockParseException(
                        "First line of the First/Genesis Block should be \"Genesis Block:\"");
            }
        }

        // Check the other blocks:
        if (1 < block.id && block.id < 5) {
            if (!lines.get(0).toLowerCase().startsWith("block")) {
                throw new BlockParseException(
                        "First line of every other Block should start with \"Block\"");
            }
        }

        // Add tests to check block was generating for line
        if (!lines.get(8).toLowerCase().startsWith("block was generating for")) {
            throw new BlockParseException("9-th line of every Block should start with \"Block was generating for\"");
        }

        String blockGeneratingTimeStr = lines.get(8).split(" ")[4];
        isNumeric = blockGeneratingTimeStr.chars().allMatch(Character::isDigit);

        if (!isNumeric) {
            throw new BlockParseException("Block generating time should be an integer number in seconds");
        }

        int blockGeneratingTime = Integer.parseInt(blockGeneratingTimeStr);
        block.blockGeneratingTime = blockGeneratingTime;

        if (block.id == 1) {
            int maxGenesisBlockGeneratingTime = 10;

            if (blockGeneratingTime < 0 || blockGeneratingTime > maxGenesisBlockGeneratingTime) {
                throw new BlockParseException(
                        "Block generating time for the Genesis block should be between 0 and " + maxGenesisBlockGeneratingTime + " seconds. " +
                                "Your program took " + blockGeneratingTime + " seconds to generate the Genesis block." + "\n" +
                                "You need to make the algorithm that generates the hash more efficient."
                );
            }
        } else {
            long timeDifferenceMillis = block.timestamp - prevTimestamp;
            // double timeDifferenceSeconds = TimeUnit.NANOSECONDS.toSeconds(timeDifferenceMillis);
            long timeDifferenceSeconds = timeDifferenceMillis / 1_000_000_000;

            if (blockGeneratingTime < 0 || blockGeneratingTime != timeDifferenceSeconds) {
                throw new BlockParseException(
                        "Block generating time for Block " + block.id + " is incorrect.\nYour program printed: \"" + lines.get(8) + "\". "
                                + "Actual block generating time: " + timeDifferenceSeconds + " seconds."
                );
            }
        }

        prevTimestamp = block.timestamp; // Update the previous timestamp after the check

        // Reset the block counter after the 5th block:
        if (blockCounter < 6) {
            blockCounter += 1;
        } else {
            blockCounter = 0;
        }

        if (blockCounter == 6) {
            prevBlockId = 0;
            blockCounter = 1;
        }

        return block;
    }


    static List<Block> parseBlocks(String output) throws BlockParseException, NoSuchAlgorithmException {
        // String[] strBlocks = output.substring(
        // output.indexOf("Block:")).split("\n\n");

        String[] strBlocks = output.split("\n\n");

        List<Block> blocks = new ArrayList<>();

        for (String strBlock : strBlocks) {
            if (strBlock.toLowerCase().startsWith("enter how many zeros the hash must start with")) {
                continue;
            }

            Block block = parseBlock(strBlock.strip());
            if (block != null) {
                blocks.add(block);
            }
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
                new TestCase<Clue>().setInput("0").setAttach(new Clue(0)),
                new TestCase<Clue>().setInput("1").setAttach(new Clue(1)),
                new TestCase<Clue>().setInput("2").setAttach(new Clue(2)),
                new TestCase<Clue>().setInput("0").setAttach(new Clue(0)),
                new TestCase<Clue>().setInput("1").setAttach(new Clue(1)),
                new TestCase<Clue>().setInput("2").setAttach(new Clue(2))
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
                            "An empty new line `\\n` must separate the prompt to enter how many zeros the hash must start with, as well as each Block.\n"
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
                        "Timestamps of blocks should increase");
            }

            if (!next.prevHash.equals(curr.hash)) {
                return new CheckResult(false, "Two hashes aren't equal, " +
                        "but should");
            }

            if (!next.hash.startsWith(clue.zeros)) {
                return new CheckResult(false,
                        "Hash should start with some zeros");
            }
        }

        return CheckResult.correct();
    }
}
