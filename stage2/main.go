package main

import (
	"crypto/sha256"
	"fmt"
	"math/rand"
	"strings"
	"time"
)

func substr(input string, start int, length int) string {
	asRunes := []rune(input)

	if start >= len(asRunes) {
		return ""
	}

	if start+length > len(asRunes) {
		length = len(asRunes) - start
	}

	return string(asRunes[start : start+length])
}

type Block struct {
	ID           int
	Timestamp    time.Time
	PreviousHash string
	Hash         string
	MagicNumber  int32
	BuildTime    int64
}

func (b *Block) Init(timestamp time.Time, previousHash string) {
	b.ID = 1
	b.Timestamp = timestamp
	b.PreviousHash = previousHash
	b.Hash = b.CalculateHash()
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

	x := fmt.Sprintf("%x", sha256Hash.Sum(nil))

	return x
}

func (b *Block) MineBlock(difficulty int) {
	if difficulty < 1 {
		b.Hash = b.CalculateHash()
	}

	for substr(b.Hash, 0, difficulty) != strings.Repeat("0", difficulty) {
		b.MagicNumber = rand.Int31()
		b.Hash = b.CalculateHash()
	}
}

func (b *Block) Print() {
	if b.ID == 1 {
		fmt.Printf("\nGenesis Block:\n")
	}

	if b.ID > 1 {
		fmt.Printf("\nBlock:\n")
	}

	fmt.Printf("Id: %d\n"+
		"Timestamp: %d\n"+
		"Magic number: %d\n"+
		"Hash of the previous block:\n%s\n"+
		"Hash of the block:\n%s\n"+
		"Block was generating for %d seconds\n",
		b.ID, b.Timestamp.UnixNano(), b.MagicNumber, b.PreviousHash, b.Hash, b.BuildTime)
}

type Blockchain struct {
	Chain      []Block
	Difficulty int
}

func (bc *Blockchain) Init(difficulty int) {
	bc.Difficulty = difficulty
	bc.Chain = []Block{bc.CreateGenesisBlock()}
}

func (bc *Blockchain) CreateGenesisBlock() Block {
	genesisBlock := Block{
		ID:           1,
		Timestamp:    time.Now(),
		PreviousHash: "0",
	}

	start := genesisBlock.Timestamp
	genesisBlock.MineBlock(bc.Difficulty)
	end := time.Now()
	genesisBlock.BuildTime = end.Unix() - start.Unix()
	return genesisBlock
}

func main() {
	fmt.Println("Enter how many zeros the hash must start with:")
	var difficulty int
	fmt.Scanln(&difficulty)

	hyperChain := new(Blockchain)
	hyperChain.Init(difficulty)

	for i := 1; i < 5; i++ {
		block := new(Block)
		start := time.Now()
		block.ID = i + 1

		block.Timestamp = time.Now()
		rand.Seed(block.Timestamp.UnixNano())
		block.MagicNumber = rand.Int31()

		block.PreviousHash = hyperChain.Chain[len(hyperChain.Chain)-1].Hash
		block.MineBlock(hyperChain.Difficulty)
		end := time.Now()
		block.BuildTime = end.Unix() - start.Unix()
		hyperChain.Chain = append(hyperChain.Chain, *block)
	}

	for _, block := range hyperChain.Chain {
		block.Print()
	}
}
