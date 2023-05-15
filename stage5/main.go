package main

import (
	"bufio"
	"crypto/ecdsa"
	"crypto/elliptic"
	cryptoRand "crypto/rand"
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
	"errors"
	"fmt"
	"log"
	"math/rand"
	"os"
	"strings"
	"time"
)

const (
	nIncreased = "N was increased to %d"
	nDecreased = "N was decreased by 1"
	nStays     = "N stays the same"
)

type Message struct {
	ID        string
	Content   string
	Signature string
	PublicKey string
}

type Block struct {
	ID           uint
	Timestamp    time.Time
	MagicNumber  int32
	PreviousHash string
	Hash         string
	Data         []Message
	BuildTime    int64
	Miner        uint
}

func (b *Block) CalculateHash() string {
	var (
		blockID           = fmt.Sprintf("%d", b.ID)
		timestamp         = fmt.Sprintf("%d", b.Timestamp.UnixNano())
		magicNumber       = fmt.Sprintf("%d", b.MagicNumber)
		previousBlockHash = b.PreviousHash
	)
	sha256Hash := sha256.New()
	sha256Hash.Write([]byte(blockID + timestamp + magicNumber + previousBlockHash))

	blockHash := fmt.Sprintf("%x", sha256Hash.Sum(nil))
	return blockHash
}

func (b *Block) GenerateMessageID(msg string, publicKey string, signature string) string {
	binaryData := []byte(fmt.Sprintf("%s%s%s", msg, publicKey, signature))

	sha256Hash1 := sha256.New()
	sha256Hash1.Write(binaryData)

	sha256Hash2 := sha256.New()
	sha256Hash2.Write(sha256Hash1.Sum(nil))

	return fmt.Sprintf("%x", sha256Hash2.Sum(nil))
}

func (b *Block) GetPrivateKey() *ecdsa.PrivateKey {
	privateKey, err := ecdsa.GenerateKey(elliptic.P256(), cryptoRand.Reader)
	if err != nil {
		log.Fatal(err)
	}
	return privateKey
}

type Blockchain struct {
	Chain   []*Block
	MemPool []Message
}

func (bc *Blockchain) Init() {
	bc.Chain = []*Block{bc.CreateGenesisBlock()}
}

func (bc *Blockchain) CreateGenesisBlock() *Block {
	timestamp := time.Now()
	rand.Seed(timestamp.UnixNano())

	var (
		blockID           = 1
		strTimestamp      = fmt.Sprintf("%d", timestamp.UnixNano())
		magicNumber       = rand.Int31()
		previousBlockHash = "0"
	)
	blockData := fmt.Sprintf("%d%s%d%s", blockID, strTimestamp, magicNumber, previousBlockHash)

	sha256Hash := sha256.New()
	sha256Hash.Write([]byte(blockData))
	hash := sha256Hash.Sum(nil)

	genesisBlock := &Block{
		ID:           1,
		Hash:         fmt.Sprintf("%x", hash),
		MagicNumber:  magicNumber,
		Timestamp:    timestamp,
		PreviousHash: "0",
	}

	// Add build time calculation for Genesis block
	start := time.Now()
	FindBlock(strings.Repeat("0", 0), genesisBlock, nil) // No difficulty for Genesis block
	buildTime := int64(time.Since(start).Seconds())
	genesisBlock.BuildTime = buildTime

	return genesisBlock
}

func (bc *Blockchain) GetBlockData() error {
	scanner := bufio.NewScanner(os.Stdin)
	fmt.Println("\nEnter a single message to send to the Blockchain:")
	scanner.Scan()
	if err := scanner.Err(); err != nil {
		return err
	}
	msgContent := scanner.Text()

	lastBlock := bc.Chain[len(bc.Chain)-1]

	msgPrivateKey := GeneratePrivateKey()

	msgSignature := SignMessage(msgContent, msgPrivateKey)

	msgPubKeyString := GetPublicKey(msgPrivateKey)

	msgID := lastBlock.GenerateMessageID(msgContent, msgPubKeyString, msgSignature)

	// Add the "pending message" to the block memory pool of the blockchain
	bc.MemPool = append(bc.MemPool, Message{
		ID:        msgID,
		Content:   msgContent,
		Signature: msgSignature,
		PublicKey: msgPubKeyString,
	})
	return nil
}

func (bc *Blockchain) GetPublicKey(block *Block) string {
	msgPublicKey := block.GetPrivateKey().PublicKey
	bytes, err := x509.MarshalPKIXPublicKey(&msgPublicKey)
	if err != nil {
		log.Fatal(err)
	}
	msgPubKeyString := base64.StdEncoding.EncodeToString(bytes)
	return msgPubKeyString
}

func (bc *Blockchain) Print(nState string) {
	// Get the last block of the blockchain
	lastBlock := bc.Chain[len(bc.Chain)-1]

	if lastBlock.ID == 1 {
		fmt.Printf("Genesis Block:\n")
	}

	if lastBlock.ID > 1 {
		fmt.Printf("\nBlock:\n")
		fmt.Printf("Created by miner%d\n", lastBlock.Miner)
	}

	fmt.Printf("Id: %d\n", lastBlock.ID)
	fmt.Printf("Timestamp: %d\n", lastBlock.Timestamp.UnixNano())
	fmt.Printf("Magic number: %d\n", lastBlock.MagicNumber)
	fmt.Printf("Hash of the previous block:\n%s\n", lastBlock.PreviousHash)
	fmt.Printf("Hash of the block:\n%s\n", lastBlock.Hash)

	if lastBlock.Data == nil {
		fmt.Printf("Block data:\n")
		fmt.Printf("No messages\n")
	} else {
		fmt.Printf("Block data:\n")
		for _, msg := range lastBlock.Data {
			fmt.Printf("%s\n", msg.Content)
			fmt.Printf("Message ID: %s\n", msg.ID)
			fmt.Printf("Public Key: %s\n", msg.PublicKey)
			fmt.Printf("Signature: %s\n", msg.Signature)
			err := VerifySignature(msg.Content, msg.Signature, msg.PublicKey)
			if err != nil {
				log.Fatal(err)
			}
		}
		lastBlock.Data = nil
	}
	fmt.Printf("Block was generating for %d seconds\n", lastBlock.BuildTime)
	fmt.Printf("%s\n", nState)

	if lastBlock.ID == 1 || lastBlock.ID < 5 {
		err := bc.GetBlockData()
		if err != nil {
			log.Fatal(err)
		}
	}
}

// ======================== HELPER FUNCTIONS ========================

func VerifySignature(msg string, signature string, publicKey string) error {
	decodedSignature, err := base64.StdEncoding.DecodeString(signature)
	if err != nil {
		return fmt.Errorf("failed to decode signature: %w", err)
	}

	decodedPublicKey, err := base64.StdEncoding.DecodeString(publicKey)
	if err != nil {
		return fmt.Errorf("failed to decode public key: %w", err)
	}

	pubKeyInterface, err := x509.ParsePKIXPublicKey(decodedPublicKey)
	if err != nil {
		return fmt.Errorf("failed to parse public key: %w", err)
	}
	pubKey := pubKeyInterface.(*ecdsa.PublicKey)

	hash := sha256.Sum256([]byte(msg))

	valid := ecdsa.VerifyASN1(pubKey, hash[:], decodedSignature)
	if !valid {
		return errors.New("invalid signature")
	}
	return nil
}

func GeneratePrivateKey() *ecdsa.PrivateKey {
	privateKey, err := ecdsa.GenerateKey(elliptic.P256(), cryptoRand.Reader)
	if err != nil {
		log.Fatal(err)
	}
	return privateKey
}

func GetPublicKey(privateKey *ecdsa.PrivateKey) string {
	publicKey, err := x509.MarshalPKIXPublicKey(&privateKey.PublicKey)
	if err != nil {
		log.Fatal(err)
	}
	return base64.StdEncoding.EncodeToString(publicKey)
}

func SignMessage(msg string, privateKey *ecdsa.PrivateKey) string {
	sha256Hash := sha256.New()
	sha256Hash.Write([]byte(msg))
	hash := sha256Hash.Sum(nil)

	bytes, err := ecdsa.SignASN1(cryptoRand.Reader, privateKey, hash[:])
	if err != nil {
		log.Fatal(err)
	}
	return base64.StdEncoding.EncodeToString(bytes)
}

func PrintGenesisBlock(difficulty int, hyperCoin *Blockchain, prefix string) (int, string) {
	difficulty++
	hyperCoin.Print(fmt.Sprintf(nIncreased, difficulty))
	prefix = strings.Repeat("0", difficulty)
	return difficulty, prefix
}

func FindBlock(prefix string, b *Block, done chan struct{}) {
	for {
		select {
		case <-done:
			return
		default:
			b.MagicNumber = rand.Int31()
			b.Hash = b.CalculateHash()
			if strings.HasPrefix(b.Hash, prefix) {
				return
			}
		}
	}
}

func (b *Block) GenerateMessageIDTimestamp() int64 {
	return time.Now().UnixNano()
}

func MineBlock(prevBlock *Block, prefix string, creator uint, next chan Block, done chan struct{}) {
	start := time.Now()
	b := Block{
		ID:           prevBlock.ID + 1,
		PreviousHash: prevBlock.Hash,
		Timestamp:    time.Now(),
	}

	FindBlock(prefix, &b, done)

	b.BuildTime = int64(time.Since(start).Seconds())
	b.Miner = creator
	next <- b
}

func MineNewBlockAndUpdateDifficulty(hyperCoin *Blockchain, prefix string, difficulty int) {
	for i := 0; i < 4; i++ {
		next := make(chan Block)
		done := make(chan struct{})

		rand.Seed(time.Now().UnixNano())
		creator := rand.Intn(10) + 1

		go MineBlock(hyperCoin.Chain[i], prefix, uint(creator), next, done)

		newBlock := <-next

		// Add the "pending messages" within the blockchain memory pool to the new block
		newBlock.Data = append(newBlock.Data, hyperCoin.MemPool...)

		// Clear the memory pool of the blockchain because no pending messages are left
		hyperCoin.MemPool = nil

		close(done)

		hyperCoin.Chain = append(hyperCoin.Chain, &newBlock)
		var nState string

		switch {
		case newBlock.BuildTime < 5:
			difficulty++
			nState = fmt.Sprintf(nIncreased, difficulty)
			prefix = strings.Repeat("0", difficulty)
		case newBlock.BuildTime > 10:
			difficulty--
			nState = nDecreased
			prefix = strings.Repeat("0", difficulty)
		default:
			nState = nStays
		}
		hyperCoin.Print(nState)
	}
}

func main() {
	var difficulty int
	var prefix string

	hyperCoin := new(Blockchain)
	hyperCoin.Init()

	difficulty, prefix = PrintGenesisBlock(difficulty, hyperCoin, prefix)

	MineNewBlockAndUpdateDifficulty(hyperCoin, prefix, difficulty)
}
