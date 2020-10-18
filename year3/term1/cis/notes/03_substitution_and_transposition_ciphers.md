
### Substitution & Transposition Ciphers

#### Content -

**Substitution Ciphers**

* Caeser Cipher
* Mono-alphabetic Substitution Ciphers
* Homophonic Substitution Ciphers
* Playfair Ciphers
* Polyalphabetic Substitution Ciphers (Vigenere Cipher)

**Transposition Ciphers**

* Rail fence cipher
* Rotating (turning) grilles
* Multiple-stage columnar transposition cipher

### Caeser Cipher -

Simply shift the alphabet a fixed number of places in either direction.

e.g.

```txt
      a b c d e f g h i ...
 +3   d e f g h i j k l ...

 Ciphertext: `khoor` = Plaintext: `hello`
```

### Mono-alphabetic substitution ciphers -

Map each letter of the alphabet uniquely to some other letter.

e.g.

```txt
      a b c d e f g h i j k l m n o ...
 =>   f a c z x g q i l b w y h e m...

 Ciphertext: `ixyym` = Plaintext: `hello`
```

### Homophonic substitution ciphers -

Mono-alphabetic ciphers are easy to break because they reflect the frequency data of the original alphabet (and may contain common bigrams/digraphs and trigraphs).

Countermeasure: provide multiple substitutes (i.e. homophones) for a single letter to make frequency analysis harder.

(Often, more frequent letters will be given larger homophone sets to better counter frequency analysis)

* Each a from alphabet A is associated  set H(a) of strings of t symbols. (Where H(a), a ∈ A are pairwise disjoint - a is not in its substitution set.)
* Replace each a with a randomly chosen string from H(a)
* To decrypt a string c of t symbols, one must determine an a ∈ A such that c ∈ H(a).
* The _key_ for the cipher is the set of sets H(a).

e.g.

```txt
A = {x,y}
H(x) = {00, 10} and H(y) = {01, 11}
The plaintext xy encrypts to one of 0001, 0011, 1001, 1011
```

There is a cost in data expansion, and more work for decryption.

The cryptanalysis is still relatively straightforward, since each element of plaintext affects only one element of ciphertext. Multi-letter patterns such as digram frequencies still survive in the ciphertext.

### Playfair Cipher

Encrypt multiple letters of plaintext, to lessen the extent of which plaintext survives in ciphertext.

Uses a 5x5 matrix of letters, constructed using a keyword.

e.g.

```txt
Pick a keyword, 'monarchy'

Construct matrix: fill in letters of keyword (minus duplicates) left-to-right, top-to-bottom, and remaining letters in alphabetical order. (letter i = j)

M  O  N  A  R
C  H  Y  B  D
E  F  G  IJ K
L  P  Q  S  T
U  V  W  X  Z

Plaintext is encrypted 2 letters at a time:
1) If a pair is a repeated letter, insert a filler e.g. X
    BALLOON -> BA LX LO ON

2) If both letters fall on the same row, replace each with the letter to the right (wrapping)
    AR => RM   [M  O  N  A  R]

3) If both letters fall in the same column, replace each with the letter below.
    MU => CM   [COL:][M  C  E  L  U]

4) Otherwise, each letter is replaced by the letter in the  same row and column of the other letter in the pair.

    HS => BP, EA => IM (or JM)

Plaintext:   TH EQ UI CK BR OW NF OX IU MP SO VE RT HE LA ZY DO G
(formatted): TH EQ UI CK BR OW NF OX IU MP SO VE RT HE LA ZY DO GA
Ciphertext:  PD GL XE DE DA NV OG AV EX OL PA UF DZ CF SM WD HR INT

To decrypt, use the inverse of rules 2 and 3, and the 4th as is (and the 1st as-is - drop any useless 'X's)
```

26x26 = 676 digrams vs 26 letters.
Needs a 676 entry freq. table to analyse (and therefore more ciphertext).

Still relatively easy to break - the structure of the underlying text is still fairly intact, and requires only a few hundred letters of ciphertext to analyse and break.

### Polyalphabetic substitution ciphers (Vigenere cipher)

Use _different_ monoalphabetic subs as one proceeds through the plaintext message. (To conceal the distribution using a _family of mappings_).

Two general features commons to these ciphers:

1) A set of related monoalphabetic substitutions is used
2) A key determines which particular rule is chosen for a given transformation

Vigenere Cipher (most well known and possibly simplest)-

* Consists of the 26 Caeser ciphers with shift of 0-25
* Each cipher is denoted by a key letter, which is the ciphertext letter that subs for the plaintext letter A.
e.g. a Caesar cipher with a shift of 3 is denoted by key value D.

Based on a _tableau_ where each row is a Caesar Cipher with incremental shift.

Needs a key as long as the message (normally a repeated keyword).

```txt
Assume:
    Sequence of plaintext letters P = p0, p1, ..., pn-1
    Key consisting of sequence K = k0, k1, ...,km-1
    (Typically m < n)

    Encryption:
        C  = C0, C1, ..., Cn-1
           = E(K, P)
           = E((k0, k1, ..., km-1), (p0, p1, ..., pn-1))
           = (p0 = k0) mod 26, (p1 + k1) mod 26, ...

        Ci = (pi + k(i mod m)) mod 26

    i.e. First letter of key added to first letter of plaintext (% 26), and so on through first m letters of plaintext. For the next m, key letters are repeated. Continue until all plaintext sequence is encrypted.

    Decryption:
        pi = (Ci - k(i mod m)) mod 26

```

### Vernam Cipher - XOR

Keyword is as long as plaintext, and has no statistical relationship to it.
(Works on binary data (bits) rather than letters, using XOR):

```txt
A  B | A XOR B
0  0      0
0  1      1
1  0      1
1  1      0
```

The idea behind it:

```
ci = pi XOR ki
pi = ci XOR ki

pi/ci/ki = ith binary digit of plaintext/ciphertext/key

Ciphertext is generated by bitwise XOR of plaintext and key
(decryption is the same bitwise operation, thanks to properties of XOR)

The essence of this cipher depends on the means of construction of this key.
(e.g. very long but repeating keyword)
```

This is difficult to break if the key is long, but still breakable with sufficient ciphertext, use of known/probable plaintext sequences, or both.

### One-Time Pads

An improvement to Vernam - use a **truly random key** that is:

* As long as the message, so it need not be repeated.
* Used to encrypt and decrypt a single message, and then discarded.

Each new message P requires a new key of same length as P.
Produces random output with no statistical relation to plaintext.

Unbreakable: C contains _no information whatsoever_ about P.
This is the only cryptosystem that exhibits "perfect secrecy".

A bruteforce search of all possible keys will yield many legible plaintexts, no way of knowing which was intended.

Practical Difficulties of OTP -

* It's hard to make a large quantity of random keys.
* Key distribution and protection - for every message to be sent, a key of equal length is needed by both sender and receiver.

Hence, there is limited utility - useful for low-bandwidth very-high-security channels (Moscow-Washington previously used this, e.g.).

## Transposition Ciphers -

Perform some sort of permutation on the plaintext letters - works on blocks of letters of the plaintext.

### Rail Fence Cipher (aka Zig-Zag, etc.)

Plaintext is written down as a sequence of diagonals and read off as a sequence of rows.

e.g.

```txt
MEET ME AFTER THE TOGA PARTY

Written with depth 2:
M  E  M  A  T  R  H  T  G  P  R  Y
  E  T  E  F  E  T  E  O  A  A  T

So that the ciphertext is:
M E M A T R H T G P R Y E T E F E T E O A A T

```

Decryption: Reconstruct the diagonal grid used to encrypt the message.

Make a grid with as many rows as the key is, and as many columns as the length of the ciphertext.

Add placeholder (-) for each rows below and fill in the next letter in the current row, until the end is reached. Repeat for each row.

Read the zig-zag back.

### Rotating (turning) grilles

Cardano grille:

Use a mask (grille) with precut holes.

The encoder writes plaintext in holes, removes mask, fills remainder with 'blind' text, retaining appearance of an innocuous message.

Decryption: recipient must possess an identical mask (or know the spacing that created it)

This is an example of steganography, but provides a basis for transposition.

---

Rotating (turning) grille:

A sheet with a grid of squares, some of which are cut out.

e.g. 6x6 grid with 9 cutouts.

Write the first 9 letters in each square cut out, left2right top2bottom.
Rotate grille by 90 degrees in predetermined direction (e.g. counterclockwise)
Write next 9 letters until grille is filled.

Read ciphertext left2right top2bottom.

Deciphering: write ciphertext in 6x6 and use rotating grid.

When enciphering, the cutouts should always land on squares that are not yet filled - there must be some procedure to select these cutouts.

### Multiple-stage columnar transposition cipher

(Single stage) Columnar transposition cipher:

Write message in a rectangle, row-by-row, and read messages off column-by-column, but _permute the order of the columns_.

This column ordering is the key to the algorithm.

e.g. with key 4312567 (+ padding to fill grid)

```txt
    Key:    4 3 1 2 5 6 7
    Plain:  a t t a c k p
            o s t p o n e
            d u n t i l t
            w o a m x y z
    Cipher: TTNAAPTMTSUOAODWCOIXKNLYPETZ

To encrypt, start with column '1', write down all letters in that column, then proceed with column labelled '2', etc.
```

Easily recognizable and attacked: same letter frequencies as original plaintext.

---

Multiple-stage columnar transposition cipher -

Perform _more than one stage_ of transposition to improve security.

e.g. run the above algorithm multiple times using the ciphertext and the new plaintext each time.
