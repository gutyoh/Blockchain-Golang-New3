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
    String prevHash;
    String hash;

    static int prevBlockId = 0;

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

        if (lines.size() != 7) {
            throw new BlockParseException("Every Block should " +
                    "contain 7 lines of data and no empty new lines within its Block data.\n" +
                    "Ensure you print each Block in the same format as the example output.");
        }

        // 1. Validate the block headers
        validateBlockHeader(block, lines);

        // 2. Validate the block id
        validateBlockId(block, lines);

        // 3. Validate the timestamp
        validateTimestamp(block, lines);

        // 4. Validate the hash of the previous block and the current hash of the block
        validatePrevHashAndHash(block, lines);

        // 5. Update the block counter
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
                    "\nYour program instead printed as the first line in Block " + blockCounter+ ": " + "\"" + lines.get(0) + "\"");
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

    private static void validatePrevHashAndHash(Block block, List<String> lines) throws BlockParseException, NoSuchAlgorithmException {
        String prevHash = lines.get(4).strip();
        String hash = lines.get(6).strip();

        if (block.id == 1) {
            if (!prevHash.equals("0")) {
                throw new BlockParseException("The \"Hash of the previous block\" of the Genesis Block must be \"0\".\nYour program instead printed: \"" + lines.get(4) + "\"");
            }
        }

        if (block.id > 1) {
            if (!(prevHash.length() == 64) || !(hash.length() == 64)) {
                throw new BlockParseException("Every subsequent Block's \"Hash of the previous block\" length should be a SHA-256 hash, which is 64 characters long." +
                        "\nYour program instead printed: \"" + lines.get(4) + "\"");
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String blockData = String.valueOf(block.id) + block.timestamp + prevHash;

            byte[] hashBytes = digest.digest(blockData.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            String calculatedHash = sb.toString();

            if (!calculatedHash.equals(hash)) {
                throw new BlockParseException("The hash of Block " + block.id + " is incorrect.\nYour program printed: \"" + lines.get(6) + "\"\n"
                        + "Expected hash of the block: " + calculatedHash + "\n" +
                        "Make sure you are only using the Block's id, timestamp and the Hash of the previous block to calculate the Hash of the block.");
            }
        }

        if (hash.equals(prevHash)) {
            throw new BlockParseException("The current hash and the previous hash in a block should be different.");
        }

        block.prevHash = prevHash;
        block.hash = hash;
    }

    private static void updateBlockCounter() {
        blockCounter++;

        if (blockCounter == 6) {
            prevBlockId = 0;
            blockCounter = 1;
        }
    }

    static List<Block> parseBlocks(String output) throws BlockParseException, NoSuchAlgorithmException {
        // Parse the blocks
        String[] lines = output.split("\n");

        List<String> strBlocks = new ArrayList<>();
        StringBuilder blockBuilder = new StringBuilder();
        boolean previousLineEmpty = false;
        int counter = 0;

        for (String line : lines) {
            if (line.isBlank()) {
                if (previousLineEmpty || (counter < lines.length - 1 && lines[counter + 1].isBlank())) {
                    throw new BlockParseException("Incorrect Block format. A single newline should separate each Block, and there should be no extra newlines within the Block data.\n" +
                            "Ensure you print each Block in the same format as the example output."
                    );
                }
                previousLineEmpty = true;

                if (blockBuilder.length() > 0) {
                    blockBuilder.append('\n');
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


public class BlockchainTest extends StageTest {

    List<String> previousOutputs = new ArrayList<>();


    @Override
    public List<TestCase> generate() {
        return List.of(
                new TestCase(),
                new TestCase()
        );
    }

    @Override
    public CheckResult check(String reply, Object clue) {

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

        Block first = blocks.get(0);
        if (!first.prevHash.equals("0")) {
            return new CheckResult(false,
                    "Previous hash of the first block should be \"0\"");
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