# Week 2 -

---

## Part 1 - General Cryptographic Schema

**Schema**:

```txt
                | Key1                       | Key2
 Plaintext      \/          Ciphertext       \/         Plaintext
--------->[ Encryption, E ]------------>[Decryption D]--------->
  P                            C                       P
```

where `E(Key1, P) = C` and `D(Key2, C) = P`.

**Terminology**:

* **Plaintext** (aka. plain text, clear text, ...) -  Text that can be read and 'understood'
* **Encryption** - transformation (function, process, ...) E that takes input plaintext P and a key, and generates a _ciphertext_.
* **Ciphertext** (encrypted text) - text that has been transformed, that needs to be processed/understood.
* **Decryption** - Transformation D that takes input ciphertext C and generates a plaintext P.
* **Cipher** - a function/algorithm for performing encryption/decryption.

**Types of algorithms**:

* SYMMETRIC: `key1=key2` (or are easily derived from each other)
* ASYMMETRIC / PUBLIC-KEY:
  * Different keys, cannot be derived from each other
  * Public key can be published without compromising private key

Encryption and Decryption should be _easy_, if keys are _known_.

Security depends on the _secrecy of the key_, **not** on the algorithm.

---

## Part 2 - A mathematical formalization of encryption/decryption

* `𝓐`, the _alphabet_, is a finite set
* `𝓜 ⊆ 𝓐*` is the _message space_. `M ∈ 𝓜` is a plaintext (message).
* `𝓒` is the _ciphertext space_ (whose alphabet may differ from 𝓜)
* `𝓚` denotes the _key space_ of _keys_.
* Each `e ∈ 𝓚` determines a bijective function from 𝓜 to 𝓒, denoted by `Eℯ`. Eℯ is the_encryption function_(or_transformation_).
 We write `Eℯ(P) = C`, or `E(e, P) = C`.
* For each `d ∈ 𝓚`, `D𝘥` denotes a bijection from 𝓒 to 𝓜. D𝘥 is the_decryption function_.

so

* Applying Eℯ is called _encryption_
* Applying D𝘥 is called _decryption_

An _encryption scheme_ (or _cipher_) consists of a set `{Ee | e ∈ 𝓚}`, and a corresponding set `{Dd | d ∈ 𝓚}` with the property that:

For each e ∈ 𝓚 there is a unique d ∈ 𝓚, such that Dd = Ee^-1, i.e.

`Dd(Ee(m)) = m for ALL m ∈ 𝓜`

("For every key *e* in the keyspace, there is another unique key *d* such that applying the decryption with key *d* to _any_ message *m* encrypted with key *e*, the original message is produced")

The keys _e_ and _d_ form a _key pair_ `(e, d)`. They CAN be identical, in a symmetric scheme.

_Constructing_ an encryption scheme, requires fixing a message space 𝓜, ciphertext space 𝓒, and a key space 𝓚, as well as the en/de-cryption transformations {Ee | e ∈ K} and {Dd | d ∈ K}.

*Example* -

M = {m1, m2, m3}

C = {c1, c2, c3}

There are 3! = 6 bijections from M to C.

Key space K = {1, 2, 3, 4, 5, 6} specifies transformations.

```txt
E1: {m1: c3, m2: c1, m3: c2},    E2: {m1: c1, m2: c3, m3: c2},
E3: {m1: c1, m2: c2, m3: c3},    E4: {m1: c3, m2: c2, m3: c1},
E5: {m1: c2, m2: c1, m3: c3},    E6: {m1: c2, m2: c3, m3: c1},
```

Suppose Alice and Bob agree on transformation E1:

To encrypt m1, Alice computes E1(m1) = c2.

Bob decrypts 2 by reversing the mapping for E1 and observing that c3 points to m1.

---

## Part 3 - Characteristics of cryptographic systems & Symmetric-key encryption

### Characteristics of Cryptographic Systems

Characterized by 3 independent dimensions

 1) _Type of Operations_ used to transform `plaintext -> ciphertext`.
 2) _Number of Keys_ used.
 3) Way in which _plaintext is processed_.

**1 - Type of Operations to transform**:

All encryption algorithms are based on two general principles:

* **Substitution**: Each element in plaintext is mapped to another element
* **Transposition**: Elements in plaintext are rearranged

All operations _must be reversible_ - **No information is lost**

Most systems involve multiple stages of substitutions and transpositions - these are referred to as **product systems**.

**2 - Number of Keys Used**:

* **Symmetric, single-key, secret-key, or conventional encryption**: both sender and receiver use the "same" key.
* **Asymmetric, two-key, or public-key encryption**: sender and receiver use different keys.

**3 - Way in which plaintext is processed**:

* **Block cipher**: processes input one block of elements at a time, produce an output block for each input block.
* **Stream cipher**: processes input elements continuously, producing output one element at a time, as it goes along.

### Symmetric-Key encryption

---

## Part 4 - Cryptanalysis and brute-force attacks

---
