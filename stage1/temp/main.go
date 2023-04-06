package main

import (
	"crypto/sha256"
	"fmt"
	"time"
)

type Block struct {
	ID           int
	Timestamp    time.Time
	PreviousHash string
	Hash         string
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
		previousBlockHash = b.PreviousHash
	)
	sha256Hash := sha256.New()
	sha256Hash.Write([]byte(blockID + timestamp + previousBlockHash))

	return fmt.Sprintf("%x", sha256Hash.Sum(nil))
}

func (b *Block) Print() {
	if b.ID == 1 {
		fmt.Printf("Genesis Block:\n")
	}

	if b.ID > 1 {
		fmt.Printf("\nBlock:\n")
	}

	fmt.Printf("Id: %d\n"+
		"Timestamp: %d\n"+
		"Hash of the previous block:\n%s\n"+
		"Hash of the block:\n%s\n",
		b.ID, b.Timestamp.UnixNano(), b.PreviousHash, b.Hash)
}

func main() {
	hyperBlock := new(Block)
	hyperBlock.Init(time.Now(), "0")
	hyperBlock.Print()

	for i := 0; i < 4; i++ {
		hyperBlock.ID++
		hyperBlock.Timestamp = time.Now()
		hyperBlock.PreviousHash = hyperBlock.Hash
		hyperBlock.Hash = hyperBlock.CalculateHash()
		hyperBlock.Print()
	}
}
