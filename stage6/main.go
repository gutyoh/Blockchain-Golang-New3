package main

import (
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
	"strings"
	"time"
)

const (
	nIncreased = "N was increased to %d"
	nDecreased = "N was decreased by 1"
	nStays     = "N stays the same"
)

type Transaction struct {
	ID string

	FromUser string
	ToUser   string

	Amount    int
	Signature string
	PublicKey string
}

type Block struct {
	ID           uint
	Timestamp    time.Time
	MagicNumber  int32
	PreviousHash string
	Hash         string
	Transactions []Transaction
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

func (b *Block) GenerateTxID(data string, pubKey string, signature ...string) string {
	binaryData := []byte(fmt.Sprintf("%s%s", data, pubKey))
	if len(signature) > 0 {
		binaryData = []byte(fmt.Sprintf("%s%s", binaryData, signature[0]))
	}

	sha256Hash1 := sha256.New()
	sha256Hash1.Write(binaryData)

	sha256Hash2 := sha256.New()
	sha256Hash2.Write(sha256Hash1.Sum(nil))

	hashData := fmt.Sprintf("%x", sha256Hash2.Sum(nil))
	return hashData
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
	MemPool []Transaction
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

func (bc *Blockchain) PromptForTxDetails() (string, string, int, error) {
	fmt.Printf("From user:\n")
	var fromUser string
	_, err := fmt.Scanln(&fromUser)
	if err != nil {
		return "", "", 0, err
	}

	fmt.Printf("To user:\n")
	var toUser string
	_, err = fmt.Scanln(&toUser)
	if err != nil {
		return "", "", 0, err
	}

	fmt.Printf("VC Amount:\n")
	var amount int
	_, err = fmt.Scanln(&amount)
	if err != nil {
		return "", "", 0, err
	}
	return fromUser, toUser, amount, nil
}

func (bc *Blockchain) GetWalletBalance(username string) int {
	// To make things simple, every user starts with 100 VC:
	balance := 100

	balance = bc.CheckValidPendingTxs(username, balance)
	balance = bc.CheckPreviousBCTxs(username, balance)
	return balance
}

func (bc *Blockchain) CheckValidPendingTxs(address string, balance int) int {
	// First check the "just added" and "valid pending transactions" in the memory pool:
	for _, block := range bc.MemPool {
		if address == block.FromUser {
			balance -= block.Amount
		}

		if address == block.ToUser {
			balance += block.Amount
		}
	}
	return balance
}

func (bc *Blockchain) CheckPreviousBCTxs(address string, balance int) int {
	// Then check for previous/older transactions in the blockchain:
	for _, block := range bc.Chain {
		for _, tx := range block.Transactions {
			if address == tx.FromUser {
				balance -= tx.Amount
			}

			if address == tx.ToUser {
				balance += tx.Amount
			}
		}
	}
	return balance
}

func (bc *Blockchain) PrintInvalidTxDetails(fromUsername string, toUsername string,
	amount int, fromUsernameCurrentBalance int) {

	fmt.Printf("Transaction is not valid\n")
	switch {
	case fromUsername == toUsername:
		fmt.Printf("You can't send VC from one user to the same user\n\n")
	case fromUsernameCurrentBalance < amount:
		fmt.Printf("User %s doesn't have enough VC to send\n", fromUsername)

		fmt.Printf("%s current balance: %d VC\n\n", fromUsername, fromUsernameCurrentBalance)
	}
}

func (bc *Blockchain) PrintValidTxDetails(fromUsername string, toUsername string,
	amount int, fromUsernameCurrentBalance int) {
	fmt.Printf("Transaction is valid\n")

	fromUsernameRemainingBalance := fromUsernameCurrentBalance - amount
	fmt.Printf("%s remaining balance: %d VC\n", fromUsername, fromUsernameRemainingBalance)

	toUsernameNewBalance := bc.GetWalletBalance(toUsername) + amount
	fmt.Printf("%s new balance: %d VC\n\n", toUsername, toUsernameNewBalance)
}

func (bc *Blockchain) ProcessValidTx(fromUsername string, toUsername string, amount int, fromUsernameCurrentBalance int) error {
	bc.PrintValidTxDetails(fromUsername, toUsername, amount, fromUsernameCurrentBalance)

	// Get the last block in the blockchain
	lastBlock := bc.Chain[len(bc.Chain)-1]

	// Generate a new private key for the transaction
	txPrivateKey := GeneratePrivateKey()

	// Sign the valid transaction
	txData := fmt.Sprintf("%s%d%s", fromUsername, amount, toUsername)
	txSignature := SignTx(txData, txPrivateKey)

	// Get the valid transaction public key
	txPublicKey := GetPublicKey(txPrivateKey)

	// Generate the valid transaction ID
	// txID := lastBlock.GenerateTxID(txData, txSignature, txPublicKey)
	txID := lastBlock.GenerateTxID(txData, txPublicKey, txSignature)

	// Add the valid "pending transaction" to the memory pool of the blockchain
	bc.MemPool = append(bc.MemPool, Transaction{
		ID:        txID,
		FromUser:  fromUsername,
		ToUser:    toUsername,
		Amount:    amount,
		Signature: txSignature,
		PublicKey: txPublicKey,
	})
	return nil
}

func (bc *Blockchain) GetTxData() error {
	// Prompt the user for the number of transactions they want to perform:
	fmt.Printf("\nEnter how many transactions you want to perform:\n")
	var numTx int
	_, err := fmt.Scanln(&numTx)
	if err != nil {
		return err
	}

	for i := 0; i < numTx; i++ {
		fromUsername, toUsername, amount, err := bc.PromptForTxDetails()
		if err != nil {
			return err
		}

		// If the user doesn't have enough VC, we don't add the transaction to the memory pool
		// If the transaction is from the same user to the same user, we don't add the transaction to the memory pool
		fromUsernameCurrentBalance := bc.GetWalletBalance(fromUsername)
		if fromUsernameCurrentBalance < amount || fromUsername == toUsername {
			bc.PrintInvalidTxDetails(fromUsername, toUsername, amount, fromUsernameCurrentBalance)
			continue
		}

		// The transaction is valid, we can further process it and make it a "pending transaction"
		// And then add it to the memory pool of the blockchain, so it is ready to be mined in the next block
		err = bc.ProcessValidTx(fromUsername, toUsername, amount, fromUsernameCurrentBalance)
		if err != nil {
			return err
		}
	}
	return nil
}

func (bc *Blockchain) Print(nState string) {
	// Get the last block of the blockchain
	lastBlock := bc.Chain[len(bc.Chain)-1]

	if lastBlock.ID == 1 {
		fmt.Printf("Genesis Block:\n")
	}

	if lastBlock.ID > 1 {
		fmt.Printf("Block:\n")
		fmt.Printf("Created by miner%d\n", lastBlock.Miner)
		bc.GenerateMiningRewardTx(lastBlock)
	}
	fmt.Printf("Id: %d\n", lastBlock.ID)
	fmt.Printf("Timestamp: %d\n", lastBlock.Timestamp.UnixNano())
	fmt.Printf("Magic number: %d\n", lastBlock.MagicNumber)
	fmt.Printf("Hash of the previous block:\n%s\n", lastBlock.PreviousHash)
	fmt.Printf("Hash of the block:\n%s\n", lastBlock.Hash)
	fmt.Printf("Block data:\n")
	if lastBlock.ID == 1 {
		fmt.Printf("No transactions\n")
	}

	for i, tx := range lastBlock.Transactions {
		if i == 0 {
			fmt.Printf("Transaction #%d (Coinbase):\n", i+1)
			fmt.Printf("%s sent %d VC to %s\n", tx.FromUser, tx.Amount, tx.ToUser)
			fmt.Printf("Transaction ID: %s\n", tx.ID)

			if lastBlock.ID > 1 {
				fmt.Printf("Public Key: %s\n", tx.PublicKey)
			}
		}
		if i > 0 {
			fmt.Printf("Transaction #%d:\n", i+1)
			fmt.Printf("%s sent %d VC to %s\n", tx.FromUser, tx.Amount, tx.ToUser)
			fmt.Printf("Transaction ID: %s\n", tx.ID)
			fmt.Printf("Public Key: %s\n", tx.PublicKey)
			fmt.Printf("Signature: %s\n", tx.Signature)

			// Verify the transaction signature
			txData := fmt.Sprintf("%s%d%s", tx.FromUser, tx.Amount, tx.ToUser)
			err := VerifyTxSignature(txData, tx.Signature, tx.PublicKey)
			if err != nil {
				log.Fatal(err)
			}
		}
	}
	fmt.Printf("Block was generating for %d seconds\n", lastBlock.BuildTime)
	fmt.Printf("%s\n", nState)

	if lastBlock.ID == 1 || lastBlock.ID < 5 {
		err := bc.GetTxData()
		if err != nil {
			log.Fatal(err)
		}
	}
}

func (bc *Blockchain) GenerateMiningRewardTx(lastBlock *Block) {
	// Generate the reward transaction for the miner in subsequent blocks
	privateKey := lastBlock.GetPrivateKey()
	publicKey := privateKey.PublicKey
	bytes, err := x509.MarshalPKIXPublicKey(&publicKey)
	if err != nil {
		log.Fatal(err)
	}
	publicKeyString := base64.StdEncoding.EncodeToString(bytes)

	txData := fmt.Sprintf("%s%s%d", "Blockchain", fmt.Sprintf("miner%d", lastBlock.Miner), 100)

	coinbaseTx := Transaction{
		// ID: lastBlock.GenerateTxID(fmt.Sprintf("%s%s%d", "Blockchain", fmt.Sprintf("miner%d", lastBlock.Miner), 100)),
		ID:        lastBlock.GenerateTxID(txData, publicKeyString),
		FromUser:  "Blockchain",
		ToUser:    fmt.Sprintf("miner%d", lastBlock.Miner),
		Amount:    100,
		PublicKey: publicKeyString,
	}

	// Add the reward transaction to the transactions of the block and make it the first transaction
	lastBlock.Transactions = append([]Transaction{coinbaseTx}, lastBlock.Transactions...)
}

// ======================== HELPER FUNCTIONS ========================

func VerifyTxSignature(data string, signature string, publicKey string) error {
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

	hash := sha256.Sum256([]byte(data))

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

func SignTx(txData string, privateKey *ecdsa.PrivateKey) string {
	sha256Hash := sha256.New()
	sha256Hash.Write([]byte(txData))
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

func AppendPendingTxsToBlockchain(hyperCoin *Blockchain, newBlock Block) {
	// If there are no pending transactions, just append the new block to the blockchain and return
	if len(hyperCoin.MemPool) == 0 {
		hyperCoin.Chain = append(hyperCoin.Chain, &newBlock)
		return
	}

	// Add the pending transactions from the blockchain memory pool to the new block, after the reward transaction
	newBlock.Transactions = append(newBlock.Transactions, hyperCoin.MemPool...)

	// Make the coinbase/reward transaction the first transaction in the block
	newBlock.Transactions = append([]Transaction{newBlock.Transactions[len(newBlock.Transactions)-1]},
		newBlock.Transactions[:len(newBlock.Transactions)-1]...)

	// Clear the memory pool of the blockchain because no pending transactions are left
	hyperCoin.MemPool = nil

	// Append the new block to the blockchain
	hyperCoin.Chain = append(hyperCoin.Chain, &newBlock)
}

func MineNewBlockAndUpdateDifficulty(hyperCoin *Blockchain, prefix string, difficulty int) {
	for i := 0; i < 4; i++ {
		next := make(chan Block)
		done := make(chan struct{})

		rand.Seed(time.Now().UnixNano())
		creator := rand.Intn(10) + 1

		go MineBlock(hyperCoin.Chain[i], prefix, uint(creator), next, done)

		newBlock := <-next

		AppendPendingTxsToBlockchain(hyperCoin, newBlock)

		close(done)
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
		// Finally, print the new Block details that were just mined and added to the blockchain:
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
