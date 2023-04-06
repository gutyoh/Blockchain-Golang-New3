import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.hyperskill.hstest.testcase.TestCase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
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

    String prevMsgID;

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


    static Block parseBlock(String strBlock) throws BlockParseException, NoSuchAlgorithmException {
        if (strBlock.length() == 0) {
            return null;
        }

        if (!(strBlock.contains("Block:")
                && strBlock.contains("Timestamp:"))) {

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

        if (lines.size() < 12 && block.id == 1) {
            throw new BlockParseException("The Genesis Block should " +
                    "contain 12 lines of data and no empty new lines within its Block data.\n" +
                    "Ensure you print each Block in the same format as the example output."
            );
        }

        if (lines.size() < 16 && block.id > 1) {
            throw new BlockParseException("Block " + block.id + " should " +
                    "contain 16 lines of data and no empty new lines within its Block data.\n" +
                    "Ensure you print each Block in the same format as the example output."
            );
        }

//        // 1. Validate the block headers:
//        validateBlockHeader(block, lines);
//
//        // 2. After validating the block header, we need to check the block data
//        // This will vary depending on the block type (Genesis or not)
//        validateMinerId(block, lines);
//
//        // 3. Validate the block id
//        int idLineIndex = block.id == 1 ? 1 : 2;
//        validateBlockId(block, lines, idLineIndex);
//
//        // 4. Validate the timestamp
//        int timestampLineIndex = block.id == 1 ? 2 : 3;
//        validateTimestamp(block, lines, timestampLineIndex);
//
//        // 5. Validate the magic number
//        int magicLineIndex = block.id == 1 ? 3 : 4;
//        validateMagicNumber(block, lines, magicLineIndex);
//
//        // 6. Validate the hash of the previous block and the current hash of the block
//        int prevHashLineIndex = block.id == 1 ? 5 : 6;
//        int hashLineIndex = block.id == 1 ? 7 : 8;
//        validatePrevHashAndHash(block, lines, prevHashLineIndex, hashLineIndex);
//
//        // 7. Validate the block data
//        int blockDataLineIndex = block.id == 1 ? 8 : 9;
//        int i = validateBlockData(block, lines, blockDataLineIndex); // Returns the line `Block was generating for ...`
//
//        // 8. Validate the Message ID
//        int msgIdLineIndex = block.id == 1 ? 9 : i;
//        validateMessageId(block, lines, msgIdLineIndex);
//
//        // 9. Validate the Signature
//        int signatureLineIndex = block.id == 1 ? 10 : i + 1;
//        validateSignature(block, lines, signatureLineIndex);
//
//        // 10. Validate the Public Key
//        int publicKeyLineIndex = block.id == 1 ? 11 : i + 2;
//        validatePublicKey(block, lines, publicKeyLineIndex);
//
//        // 11. Validate the block generation time
//        int blockGeneratingTimeLineIndex = block.id == 1 ? 10 : i;
//        i = validateGenerationTime(block, lines, blockGeneratingTimeLineIndex); // Returns the line `N ...`
//
//        // 12. Validate if N increased, decreased or stayed the same
//        int nLineIndex = block.id == 1 ? 11 : i + 1;
//        validateN(block, lines, nLineIndex);
//
//        // 13. Update the block counter
//        updateBlockCounter();

        // Calculate the line offset depending on whether it's the Genesis block or not
        int lineOffset = block.id == 1 ? 0 : 1;

        // 1. Validate the block headers:
        validateBlockHeader(block, lines);

        // 2. After validating the block header, we need to check the block data
        // This will vary depending on the block type (Genesis or not)
        validateMinerId(block, lines);

        // 3. Validate the block id
        validateBlockId(block, lines, 1 + lineOffset);

        // 4. Validate the timestamp
        validateTimestamp(block, lines, 2 + lineOffset);

        // 5. Validate the magic number
        validateMagicNumber(block, lines, 3 + lineOffset);

        // 6. Validate the hash of the previous block and the current hash of the block
        validatePrevHashAndHash(block, lines, 5 + lineOffset, 7 + lineOffset);

        // 7. Validate the block data
        int i = validateBlockData(block, lines, 8 + lineOffset); // Returns the line `Block was generating for ...`

        // 8. Validate the Message ID
        validateMessageId(block, lines, 9 + lineOffset);

        // 9. Validate the Signature
        validateSignature(block, lines, i);

        // 10. Validate the Public Key
        validatePublicKey(block, lines, i + 1);

        // 11. Validate the block generation time
        i = validateGenerationTime(block, lines, i); // Returns the line `N ...`

        // 12. Validate if N increased, decreased or stayed the same
        validateN(block, lines, i + 1);

        // 13. Update the block counter
        updateBlockCounter();

        return block;
    }

    private static void checkLineStartsWith(Block block, List<String> lines, int lineIndex, String expectedStart) throws BlockParseException {
        if (!lines.get(lineIndex).toLowerCase().startsWith(expectedStart.toLowerCase())) {
            throw new BlockParseException("Line " + (lineIndex + 1) + " of " + (block.id == 1 ? "the Genesis Block" : "every subsequent Block") + " should start with \"" + expectedStart + "\"" +
                    "\nYour program instead printed as line " + (lineIndex + 1) + " in Block " + blockCounter + ": " + "\"" + lines.get(lineIndex) + "\"");
        }
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

    private static void validateMinerId(Block block, List<String> lines) throws BlockParseException {
        if (block.id > 1 && block.id <= 5) {
            if (!lines.get(1).toLowerCase().startsWith("created by")) {
                throw new BlockParseException("Second line of every subsequent Block should start with \"Created by\"");
            }
            minerIds.add(lines.get(1));
        }
    }

    private static void validateBlockId(Block block, List<String> lines, int idLineIndex) throws BlockParseException {
        checkLineStartsWith(block, lines, idLineIndex, "id:");
        String idStr = lines.get(idLineIndex).split(":")[1].strip();
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
                                "Your program printed as the second line in the Genesis Block: " + "\"" + lines.get(idLineIndex) + "\""
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
                            "\nYour program printed as the second line in Block " + blockCounter + ": " + "\"" + lines.get(idLineIndex) + "\""
            );
        }

        if (!lines.get(0).toLowerCase().startsWith("genesis block") && block.id != prevBlockId + 1) {
            throw new BlockParseException(
                    "Each subsequent Block Id should increment by 1. Found Block Id: " + block.id + ", expected Block Id: " + (prevBlockId + 1) +
                            "\nYour program printed as the second line in Block " + blockCounter + ": " + "\"" + lines.get(idLineIndex) + "\""
            );
        }

        prevBlockId = block.id; // Update the previous block id after the check
    }

    private static void validateTimestamp(Block block, List<String> lines, int timestampLineIndex) throws BlockParseException {
        boolean isNumeric;
        checkLineStartsWith(block, lines, timestampLineIndex, "timestamp:");

        String timestampStr = lines.get(timestampLineIndex).split(":")[1].strip();
        isNumeric = timestampStr.chars().allMatch(Character::isDigit);

        if (!isNumeric) {
            throw new BlockParseException("Timestamp should be a number");
        }

        long timestamp = Long.parseLong(timestampStr);

        // Check if the timestamp is a valid UnixNano timestamp by ensuring it has 19 digits
        if (timestampStr.length() != 19) {
            throw new BlockParseException("Timestamp should be a UnixNano timestamp.\n" +
                    "Your program printed in Block " + block.id + ": " + "\"" + lines.get(timestampLineIndex) + "\"\n" +
                    "Make sure you're using UnixNano timestamps and not Unix, UnixMilli, UnixMicro or other timestamps.");
        }

        block.timestamp = timestamp;
    }

    private static void validateMagicNumber(Block block, List<String> lines, int magicNumberLineIndex) throws BlockParseException {
        checkLineStartsWith(block, lines, magicNumberLineIndex, "magic number:");

        String magic = lines.get(magicNumberLineIndex).split(":")[1].strip();
        boolean isNegative = magic.startsWith("-");
        if (isNegative) {
            magic = magic.substring(1);
        }
        boolean isNumeric = magic.chars().allMatch(Character::isDigit);

        if (!isNumeric) {
            throw new BlockParseException("The magic number your program printed is not a number.\n" +
                    "Your program printed in Block " + block.id + ": " + "\"" + lines.get(magicNumberLineIndex) + "\"");
        }

        long magicNumber = Long.parseLong(magic);
        if (isNegative) {
            throw new BlockParseException("The magic number is negative. It should be a positive 32-bit signed integer number.\n" +
                    "Your program printed in Block " + block.id + ": " + "\"" + lines.get(magicNumberLineIndex) + "\"");
        } else if (magicNumber > Integer.MAX_VALUE) {
            throw new BlockParseException("The magic number is out of range. It should be a positive 32-bit signed integer number.\n" +
                    "Your program printed in Block " + block.id + ": " + "\"" + lines.get(magicNumberLineIndex) + "\"");
        }

        block.magic = magicNumber;
    }

    private static void validatePrevHashAndHash(Block block, List<String> lines, int prevHashLineIndex, int hashLineIndex) throws BlockParseException, NoSuchAlgorithmException {
        checkLineStartsWith(block, lines, prevHashLineIndex - 1, "Hash of the previous block:");
        checkLineStartsWith(block, lines, hashLineIndex - 1, "Hash of the block:");

        String prevHash = lines.get(prevHashLineIndex).strip();
        String hash = lines.get(hashLineIndex).strip();

        if (block.id == 1) {
            if (!prevHash.equals("0")) {
                throw new BlockParseException("The \"Hash of the previous block\" of the Genesis Block must be \"0\".\nYour program instead printed: \"" + lines.get(prevHashLineIndex) + "\"");
            }
        }

        if (block.id > 1) {
            if (!(prevHash.length() == 64) || !(hash.length() == 64)) {
                throw new BlockParseException("Every subsequent Block's \"Hash of the previous block\" length should be a SHA-256 hash, which is 64 characters long." +
                        "\nYour program instead printed: \"" + lines.get(prevHashLineIndex) + "\"");
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String blockData = String.valueOf(block.id) + String.valueOf(block.timestamp) + String.valueOf(block.magic) + prevHash;

            byte[] hashBytes = digest.digest(blockData.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            String calculatedHash = sb.toString();

            if (!calculatedHash.equals(hash)) {
                throw new BlockParseException("The hash of Block " + block.id + " is incorrect.\nYour program printed: \"" + lines.get(hashLineIndex) + "\"\n"
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

    private static int validateBlockData(Block block, List<String> lines, int blockDataLineIndex) throws BlockParseException {
        int i = 0; // Used to iterate through the blockDataMessage array

        if (block.id == 1) {
            checkLineStartsWith(block, lines, blockDataLineIndex, "block data:");

            if (!lines.get(blockDataLineIndex + 1).toLowerCase().contains("no messages")) {
                throw new BlockParseException("10-th line of the Genesis Block " +
                        "should contain \"no messages\"\n" +
                        "Your program instead printed as the 10-th line in Block " + blockCounter + ": " + "\"" + lines.get(blockDataLineIndex + 1) + "\"");
            }
        }

        if (block.id > 1 && block.id <= 5) {
            checkLineStartsWith(block, lines, blockDataLineIndex, "block data:");

            i = 10;

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
        }

        return i;
    }

    private static int validateMessageId(Block block, List<String> lines, int msgIdLineIndex) throws BlockParseException, NoSuchAlgorithmException {
        if (block.id == 1) {
            return msgIdLineIndex;
        }

        checkLineStartsWith(block, lines, msgIdLineIndex, "message id");

        String messageID = lines.get(msgIdLineIndex).split(":")[1].strip();

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String binaryData = String.valueOf(block.blockDataMessage[0] + block.timestamp);

        byte[] hashBytes = digest.digest(binaryData.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }

        String calculatedHash = sb.toString();

        if (!calculatedHash.equals(messageID)) {
            throw new BlockParseException("The message ID of Block " + block.id + " is incorrect.\nYour program printed: \"" + lines.get(msgIdLineIndex) + "\"\n"
                    + "Expected Message ID: \"" + calculatedHash + "\"\n" +
                    "Make sure you are only using the Block's id, timestamp, magic number and the Hash of the previous block to calculate the Hash of the block.");
        }

        return msgIdLineIndex;
    }

//    private static int validateSignature(Block block, List<String> lines, int signatureLineIndex) throws BlockParseException {
//        checkLineStartsWith(block, lines, signatureLineIndex, "signature");
//
//        // Get the signature value after the `:` colon in the string `Signature:`
//        String signature = lines.get(signatureLineIndex).split(":")[1].strip();
//
//        if (signature == null) {
//            throw new BlockParseException("Make sure you write the signature after the `Signature:` string.\n" +
//                    "For example: \"Signature:MEUCIBFU...\"" +
//
//                    "\n" + "Your program instead printed in Block" +
//                    block.id + " an unexpected line: " + lines.get(signatureLineIndex));
//        }
//
//        // TODO validate the signature with a stronger check:
//        // Check if the signature starts with `ME` and has a length of 96 characters:
//        if (!signature.startsWith("ME") || signature.length() != 96) {
//            throw new BlockParseException("The Signature should be ASN.1 encoded and " +
//                    "have a length of 96 characters.\n" +
//                    "Your Signature: " + signature + "\n" +
//                    "Your Signature length: " + signature.length());
//        }
//
//        return signatureLineIndex;
//    }

    private static int validateSignature(Block block, List<String> lines, int signatureLineIndex) throws BlockParseException {
        if (block.id == 1) {
            return signatureLineIndex;
        }

        checkLineStartsWith(block, lines, signatureLineIndex, "signature");

        // Get the signature value after the `:` colon in the string `Signature:`
        String signature = lines.get(signatureLineIndex).split(":")[1].strip();

        if (signature == null) {
            throw new BlockParseException("Make sure you write the signature after the `Signature:` string.\n" +
                    "For example: \"Signature:MEUCIBFU...\"" +

                    "\n" + "Your program instead printed in Block" +
                    block.id + " an unexpected line: " + lines.get(signatureLineIndex));
        }

        // Check if the signature is valid base64 string
        if (!signature.matches("^[A-Za-z0-9+/]+={0,2}$")) {
            throw new BlockParseException("The Signature should be a valid base64 encoded string.\n" +
                    "Your Signature: " + signature);
        }

        // Decode the signature
        byte[] decodedSignature;
        try {
            decodedSignature = Base64.getDecoder().decode(signature);
        } catch (IllegalArgumentException e) {
            throw new BlockParseException("The Signature is not a valid base64 encoded string.\n" +
                    "Your Signature: " + signature);
        }

        // Check the length of the decoded signature (For ECDSA P-256, the signature length can vary. In ASN.1 DER encoding, it will typically be between 68 and 72 bytes)
        if (decodedSignature.length < 68 || decodedSignature.length > 72) {
            throw new BlockParseException("The Signature should have a length between 68 and 72 bytes after decoding.\n" +
                    "Your Decoded Signature length: " + decodedSignature.length);
        }

        return signatureLineIndex;
    }

//    private static int validatePublicKey(Block block, List<String> lines, int publicKeyLineIndex) throws BlockParseException {
//        checkLineStartsWith(block, lines, publicKeyLineIndex, "public key");
//
//        // Get the Public Key value after the `:` colon in the string `Public Key:`
//        String publicKey = lines.get(publicKeyLineIndex).split(":")[1].trim();
//
//        if (publicKey.isEmpty()) {
//            throw new BlockParseException("Make sure you write the public key after the \"Public Key:\" string.\n" +
//                    "For example \"Public Key: MFkw...\"");
//        }
//
//        // TODO validate the public key with a stronger check:
//        // check if the public key starts with MF and has a length of 120 or 124 characters
//        if (!publicKey.startsWith("MF") || (publicKey.length() != 120 && publicKey.length() != 124)) {
//            throw new BlockParseException("The Public Key should be in the PKIX, ASN.1 DER form and have a " +
//                    "length of 120 or 124 characters.\n" +
//                    "Your Public Key: " + publicKey + "\n" +
//                    "Your Public Key length: " + publicKey.length());
//        }
//
//        return publicKeyLineIndex;
//    }

    private static int validatePublicKey(Block block, List<String> lines, int publicKeyLineIndex) throws BlockParseException {
        if (block.id == 1) {
            return publicKeyLineIndex;
        }

        checkLineStartsWith(block, lines, publicKeyLineIndex, "public key");

        // Get the public key value after the `:` colon in the string `Public Key:`
        String publicKey = lines.get(publicKeyLineIndex).split(":")[1].strip();

        if (publicKey == null) {
            throw new BlockParseException("Make sure you write the public key after the `Public Key:` string.\n" +
                    "For example: \"Public Key:MFkwEwYHKoZI...\"" +

                    "\n" + "Your program instead printed in Block" +
                    block.id + " an unexpected line: " + lines.get(publicKeyLineIndex));
        }

        // Check if the public key is valid base64 string
        if (!publicKey.matches("^[A-Za-z0-9+/]+={0,2}$")) {
            throw new BlockParseException("The Public Key should be a valid base64 encoded string.\n" +
                    "Your Public Key: " + publicKey);
        }

        // Decode the public key
        byte[] decodedPublicKey;
        try {
            decodedPublicKey = Base64.getDecoder().decode(publicKey);
        } catch (IllegalArgumentException e) {
            throw new BlockParseException("The Public Key is not a valid base64 encoded string.\n" +
                    "Your Public Key: " + publicKey);
        }

        // Check the length of the decoded public key (For ECDSA P-256, the decoded public key length should typically be around 91 bytes)
        if (decodedPublicKey.length != 91) {
            throw new BlockParseException("The Public Key should have a length of 91 bytes after decoding.\n" +
                    "Your Decoded Public Key length: " + decodedPublicKey.length);
        }

        // Check if the decoded public key starts with the expected ASN.1 encoded sequence (0x30 or '0')
        if (decodedPublicKey[0] != 0x30) {
            throw new BlockParseException("The Public Key should start with the expected ASN.1 encoded sequence (0x30 or '0').\n" +
                    "Your Decoded Public Key: " + Arrays.toString(decodedPublicKey));
        }

        return publicKeyLineIndex;
    }


    private static int validateGenerationTime(Block block, List<String> lines, int blockGeneratingTimeLineIndex) throws BlockParseException {
        checkLineStartsWith(block, lines, blockGeneratingTimeLineIndex, "block was generating for");

        String blockGeneratingTimeStr = lines.get(blockGeneratingTimeLineIndex).split(" ")[4];
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
            long timeDifferenceNanos = block.timestamp - prevTimestamp;

            long timeDifferenceSeconds = TimeUnit.NANOSECONDS.toSeconds(timeDifferenceNanos);

            if (blockGeneratingTime < 0 || blockGeneratingTime != timeDifferenceSeconds) {
                throw new BlockParseException(
                        "Block generating time for Block " + block.id + " is incorrect.\nYour program printed: \"" + lines.get(blockGeneratingTimeLineIndex) + "\". "
                                + "Actual Block generation time: " + timeDifferenceSeconds + " seconds."
                );
            }
        }

        prevTimestamp = block.timestamp; // Update the previous timestamp after the check

        return blockGeneratingTimeLineIndex;
    }

//    private static void validateN(Block block, List<String> lines, int nLineIndex) throws BlockParseException {
//        checkLineStartsWith(block, lines, nLineIndex, "N");
//
//        String nLineContent = lines.get(nLineIndex).toLowerCase();
//
//        if (nLineContent.contains("increase")) {
//            N += 1;
//        } else if (nLineContent.contains("decrease")) {
//            N -= 1;
//            if (N < 0) {
//                throw new BlockParseException("N was decreased below zero!");
//            }
//        } else if (!nLineContent.contains("same")) {
//            throw new BlockParseException("The last line of every block" +
//                    "must state N increased, decreased, or stayed the same.");
//        }
//
//        int expectedLastLineIndex = block.id == 1 ? 11 : nLineIndex;
//        if (lines.size() - 1 != expectedLastLineIndex) {
//            throw new BlockParseException("Your program printed in Block " + block.id +
//                    " after the line: \"N was increased/decreased/stays the same\"\n" +
//                    "an additional and unexpected line: " + lines.get(lines.size() - 1));
//        }
//    }

    private static void validateN(Block block, List<String> lines, int nLineIndex) throws BlockParseException {
        checkLineStartsWith(block, lines, nLineIndex, "N");

        String nLineContent = lines.get(nLineIndex).toLowerCase();
        String operation;

        if (nLineContent.contains("increase")) {
            operation = "increase";
        } else if (nLineContent.contains("decrease")) {
            operation = "decrease";
        } else if (nLineContent.contains("same")) {
            operation = "same";
        } else {
            throw new BlockParseException("The last line of every block" +
                    "must state N increased, decreased, or stayed the same.");
        }

        switch (operation) {
            case "increase":
                N += 1;
                break;
            case "decrease":
                N -= 1;
                if (N < 0) {
                    throw new BlockParseException("N was decreased below zero!");
                }
                break;
            case "same":
                break; // Do nothing
        }

        int expectedLastLineIndex = block.id == 1 ? 11 : nLineIndex;
        if (lines.size() - 1 != expectedLastLineIndex) {
            throw new BlockParseException("Your program printed in Block " + block.id +
                    " after the line: \"N was increased/decreased/stays the same\"\n" +
                    "an additional and unexpected line: " + lines.get(lines.size() - 1));
        }
    }

    private static void updateBlockCounter() {
        blockCounter++;

        if (blockCounter == 6) {
            prevBlockId = 0;
            blockCounter = 1;
        }
    }


    static List<Block> parseBlocks(String output) throws BlockParseException, NoSuchAlgorithmException {
        minerIds = new ArrayList<String>();
        N = 0;

        // Parse the blocks
        String[] lines = output.split("\n");

        List<String> strBlocks = new ArrayList<>();
        StringBuilder blockBuilder = new StringBuilder();
        boolean previousLineEmpty = false;
        int counter = 0;

        for (String line : lines) {
            if (line.isBlank()) {
                if (previousLineEmpty || (counter < lines.length - 1 && lines[counter + 1].isBlank())) {
                    throw new BlockParseException("Incorrect Block output format. A single newline should separate each Block, and there should be no extra newlines within the Block data.\n" +
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