import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.hyperskill.hstest.testcase.TestCase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
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

    int generationTime;

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
                    "contain 9 lines of data and no empty new lines within its Block data.\n" +
                    "Ensure you print each Block in the same format as the example output."
            );
        }

        // 1. Validate the block headers
        validateBlockHeader(block, lines);

        // 2. Validate the block id
        validateBlockId(block, lines);

        // 3. Validate the timestamp
        validateTimestamp(block, lines);

        // 4. Validate the magic number
        validateMagicNumber(block, lines);

        // 4. Validate the previous hash and the current hash
        validatePrevHashAndHash(block, lines);

        // 5. Validate the block generating time
        validateBlockGenerationTime(block, lines);

        // 6. Update the block counter
        updateBlockCounter();

        return block;
    }

    private static void validateBlockHeader(Block block, List<String> lines) throws BlockParseException {
        if (blockCounter == 1) {
            if (!lines.get(0).toLowerCase().startsWith("genesis block")) {
                throw new BlockParseException("The first line of the first block in the blockchain should be \"Genesis Block:\"" +
                        "\nYour program instead printed as the first line in Block " + blockCounter + ": " + "\"" + lines.get(0) + "\"");
            }
        }

        if (blockCounter > 1 && !lines.get(0).toLowerCase().startsWith("block")) {
            throw new BlockParseException("Every subsequent Block's first line should be \"Block:\"" +
                    "\nYour program instead printed as the first line in Block " + blockCounter + ": " + "\"" + lines.get(0) + "\"");
        }
    }

    private static void validateBlockId(Block block, List<String> lines) throws BlockParseException {
        if (!lines.get(1).toLowerCase().startsWith("id:")) {
            throw new BlockParseException("Second line of every Block should start with \"Id:\"");
        }

        String idStr = lines.get(1).split(":")[1].strip();
        boolean isNumeric = idStr.chars().allMatch(Character::isDigit);

        if (!isNumeric) {
            throw new BlockParseException("Id should be a number");
        }

        block.id = Integer.parseInt(idStr);

        // Check if the current block id is not 1 greater than the previous block id
        if (lines.get(0).toLowerCase().startsWith("genesis block")) {
            if (block.id != 1) {
                throw new BlockParseException(
                        "The Genesis Block must have Id: 1\n" +
                                "Your program printed as the second line in the Genesis Block: " + "\"" + lines.get(1) + "\""
                );
            }
            if (block.id != prevBlockId + 1) {
                throw new BlockParseException(
                        "You have printed the Genesis Block more than once. " +
                                "The Genesis Block should be printed only once at the beginning of the blockchain."
                );
            }
        }

        if (!lines.get(0).toLowerCase().startsWith("genesis block") && block.id <= 0) {
            throw new BlockParseException(
                    "Each subsequent Block Id should increment by 1. Found Block Id: " + block.id + ", expected Block Id: " + (prevBlockId + 1) +
                            "\nYour program printed as the second line in Block " + blockCounter + ": " + "\"" + lines.get(1) + "\""
            );
        }

        if (!lines.get(0).toLowerCase().startsWith("genesis block") && block.id != prevBlockId + 1) {
            throw new BlockParseException(
                    "Each subsequent Block Id should increment by 1. Found Block Id: " + block.id + ", expected Block Id: " + (prevBlockId + 1) +
                            "\nYour program printed as the second line in Block " + blockCounter + ": " + "\"" + lines.get(1) + "\""
            );
        }

        prevBlockId = block.id; // Update the previous block id after the check
    }

    private static void validateTimestamp(Block block, List<String> lines) throws BlockParseException {
        boolean isNumeric;

        if (!lines.get(2).toLowerCase().startsWith("timestamp:")) {
            throw new BlockParseException("Third line of every Block should start with \"Timestamp:\"");
        }

        String timestampStr = lines.get(2).split(":")[1].strip();
        isNumeric = timestampStr.chars().allMatch(Character::isDigit);

        if (!isNumeric) {
            throw new BlockParseException("Timestamp should be a number");
        }

        long timestamp = Long.parseLong(timestampStr);

        // Check if the timestamp is a valid UnixNano timestamp by ensuring it has 19 digits
        if (timestampStr.length() != 19) {
            throw new BlockParseException("Timestamp should be a UnixNano timestamp.\n" +
                    "Your program printed in Block " + block.id + ": " + "\"" + lines.get(2) + "\"\n" +
                    "Make sure you're using UnixNano timestamps and not Unix, UnixMilli, UnixMicro or other timestamps.");
        }

        block.timestamp = timestamp;
    }

    private static void validateMagicNumber(Block block, List<String> lines) throws BlockParseException {
        if (!lines.get(3).toLowerCase().startsWith("magic number:")) {
            throw new BlockParseException("Fourth line of every Block should start with \"Magic number:\"");
        }

        String magic = lines.get(3).split(":")[1].strip();
        boolean isNegative = magic.startsWith("-");
        if (isNegative) {
            magic = magic.substring(1);
        }
        boolean isNumeric = magic.chars().allMatch(Character::isDigit);

        if (!isNumeric) {
            throw new BlockParseException("The magic number your program printed is not a number.\n" +
                    "Your program printed in Block " + block.id + ": " + "\"" + lines.get(3) + "\"");
        }

        long magicNumber = Long.parseLong(magic);
        if (isNegative) {
            throw new BlockParseException("The magic number is negative. It should be a positive 32-bit signed integer number.\n" +
                    "Your program printed in Block " + block.id + ": " + "\"" + lines.get(3) + "\"");
        } else if (magicNumber > Integer.MAX_VALUE) {
            throw new BlockParseException("The magic number is out of range. It should be a positive 32-bit signed integer number.\n" +
                    "Your program printed in Block " + block.id + ": " + "\"" + lines.get(3) + "\"");
        }

        block.magic = magicNumber;
    }


    private static void validatePrevHashAndHash(Block block, List<String> lines) throws BlockParseException, NoSuchAlgorithmException {
        String prevHash = lines.get(5).strip();
        String hash = lines.get(7).strip();

        if (block.id == 1) {
            if (!prevHash.equals("0")) {
                throw new BlockParseException("The \"Hash of the previous block\" of the Genesis Block must be \"0\".\nYour program instead printed: \"" + lines.get(5) + "\"");
            }
        }

        if (block.id > 1) {
            if (!(prevHash.length() == 64) || !(hash.length() == 64)) {
                throw new BlockParseException("Every subsequent Block's \"Hash of the previous block\" length should be a SHA-256 hash, which is 64 characters long." +
                        "\nYour program instead printed: \"" + lines.get(5) + "\"");
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String blockData = String.valueOf(block.id) + block.timestamp + block.magic + prevHash;

            byte[] hashBytes = digest.digest(blockData.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            String calculatedHash = sb.toString();

            if (!calculatedHash.equals(hash)) {
                throw new BlockParseException("The hash of Block " + block.id + " is incorrect.\nYour program printed: \"" + lines.get(7) + "\"\n"
                        + "Expected Hash of the block: \"" + calculatedHash + "\"\n" +
                        "Make sure you are only using the Block's id, timestamp, magic number and the Hash of the previous block to calculate the Hash of the block.");
            }
        }

        if (hash.equals(prevHash)) {
            throw new BlockParseException("The current hash and the previous hash in a block should be different.");
        }

        block.prevHash = prevHash;
        block.hash = hash;
    }

    private static void validateBlockGenerationTime(Block block, List<String> lines) throws BlockParseException {
        if (!lines.get(8).toLowerCase().startsWith("block was generating for")) {
            throw new BlockParseException("9-th line of every Block should start with \"Block was generating for\"");
        }

        String blockGeneratingTimeStr = lines.get(8).split(" ")[4];
        boolean isNumeric = blockGeneratingTimeStr.chars().allMatch(Character::isDigit);

        if (!isNumeric) {
            throw new BlockParseException("Block generating time should be an integer number in seconds");
        }

        int blockGeneratingTime = Integer.parseInt(blockGeneratingTimeStr);
        block.generationTime = blockGeneratingTime;

        if (block.id == 1) {
            int maxGenesisBlockGeneratingTime = 5;

            if (blockGeneratingTime < 0 || blockGeneratingTime > maxGenesisBlockGeneratingTime) {
                throw new BlockParseException(
                        "Block generating time for the Genesis block should be between 0 and " + maxGenesisBlockGeneratingTime + " seconds. " +
                                "Your program took " + blockGeneratingTime + " seconds to generate the Genesis Block." + "\n" +
                                "You need to make the algorithm that generates the Hash of the block more efficient."
                );
            }
        }

        if (block.id > 1) {
            long timeDifferenceMillis = block.timestamp - prevTimestamp;
            // double timeDifferenceSeconds = TimeUnit.NANOSECONDS.toSeconds(timeDifferenceMillis);
            long timeDifferenceSeconds = timeDifferenceMillis / 1_000_000_000;

            if (blockGeneratingTime < 0 || blockGeneratingTime != timeDifferenceSeconds) {
                throw new BlockParseException(
                        "Block generating time for Block " + block.id + " is incorrect.\nYour program printed: \"" + lines.get(8) + "\". "
                                + "Actual Block generation time: " + timeDifferenceSeconds + " seconds."
                );
            }
        }

        prevTimestamp = block.timestamp; // Update the previous timestamp after the check
    }

    private static void updateBlockCounter() {
        blockCounter++;

        if (blockCounter == 6) {
            prevBlockId = 0;
            blockCounter = 1;
        }
    }

    static void parsePrompt(String output, String requiredPrompt) throws BlockParseException {
        boolean promptFound = false;

        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.toLowerCase().startsWith(requiredPrompt.toLowerCase())) {
                promptFound = true;
                break;
            }
        }

        if (!promptFound) {
            throw new BlockParseException("Incorrect input prompt used.\nYour program should prompt the user \"" + requiredPrompt + ":\" but instead prompted: \"" + output + "\"");
        }
    }

    static List<Block> parseBlocks(String output) throws BlockParseException, NoSuchAlgorithmException {
        // First, validate the prompt
        String requiredPrompt = "Enter how many zeros the hash must start with";
        parsePrompt(output, requiredPrompt);

        // Then, parse the blocks
        String[] lines = output.split("\n");

        boolean promptFound = false;
        List<String> strBlocks = new ArrayList<>();
        StringBuilder blockBuilder = new StringBuilder();
        boolean previousLineEmpty = false;
        int counter = 0;

        for (String line : lines) {
            if (!promptFound) {
                if (line.toLowerCase().startsWith(requiredPrompt.toLowerCase())) {
                    promptFound = true;

                    if (counter + 2 < lines.length && lines[counter + 2].isBlank()) {
                        throw new BlockParseException("Incorrect block format. There should be exactly one empty newline after the prompt.\n");
                    }
                }
                counter++;
                continue;
            }

            if (line.isBlank()) {
                if (previousLineEmpty && (lines[counter + 1].isBlank() || counter == lines.length - 1)) {
                    throw new BlockParseException("Incorrect Block format. A single newline should separate each Block, and there should be no extra newlines within the Block data.\n" +
                            "Ensure you print each Block in the same format as the example output."
                    );
                }
                previousLineEmpty = true;

                if (blockBuilder.length() > 0) {
                    blockBuilder.append('\n'); // Add the newline before adding to strBlocks.
                    strBlocks.add(blockBuilder.toString().strip());
                    blockBuilder.setLength(0);
                }
            } else {
                blockBuilder.append(line).append('\n');
                previousLineEmpty = false;
            }
            counter++;
        }

        if (blockBuilder.length() > 0) {
            blockBuilder.append('\n'); // Add the newline for the last block.
            strBlocks.add(blockBuilder.toString().strip());
        }

        List<Block> blocks = new ArrayList<>();
        for (String strBlock : strBlocks) {
            Block block = parseBlock(strBlock);
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
