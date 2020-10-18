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

### Symmetric-Key encryption (symmetric cipher model)

An encryption scheme `{Ee | e ∈ K}` and `{Dd | d ∈ K}` is a _symmetric-key_, if for each associated pair `(e,d)` it is computationally 'easy' to determine _d_ knowing only _e_, and vice versa. In practice `e = d`.

This is the most widely used model. Also known as: _secret-key, single-key, one-key, shared-key, conventional encryption_.

Sender and recipient share a common key.

All classical encryption algorithms are symmetric-key (prior to public-key in the 1970s, it was the only type of encryption).

**Requirements for secure use of symmetric encryption**:

1) A strong encryption algorithm
    * At minimum: an attacker who knows the algorithm should not be able to determine the key or decipher ciphertext even if they can access the ciphertext.
    * Stronger: an attacker shouldn't be able to discover key or decrypt future messages even if they have access to multiple plaintext and their encrypted ciphertexts.
2) Sender and receiver must _obtain_ copies of secret key in a secure fashion(e.g. a secure channel) and must _keep_ it secure.
    * If someone can discover the key and knows the algorithm, they can read all communication.

We don't need to keep the algorithm secret, only the key.

---

## Part 4 - Cryptanalysis and brute-force attacks

Typical objective of attacking an encryption system:

* Not simply to recover plaintext of a _single_ ciphertext
* ...also to recover the key in use, so all future messages are compromised

General Attack Approaches:

1) **Cryptanalysis** - Exploits characteristics of an algorithm to deduce a specific plaintext or the key being used.
2) **Brute-force attack** - Trying every possible key on a piece of ciphertext until a translation is found. (On average, half of all keys must be tried)

**Brute-force attacks**

These are always possible (if the algorithm is known), since you can simply try every possible key. Doesn't mean that it is feasible to try every combination however.

The cost heavily depends on key size.
e.g.

```txt
32 bit key has 2^32 alternative keys (4.3E9) - 2ms at 10^6 decrypts/μs
128 bit key has 2^128 alternative keys (3.4E38) - 5.4E18 years at 10^6 decrypts/μs
```

(1 decryption per microsecond is reasonable, 10^6/μs may be possible in the future - DES would no longer be computationally secure!)

Example key sizes:

* DES = 56
* Triple DES = 168
* AES >= 128

For substitution codes using 26 characters in a permutation, there are 26! = 4x10^26 possible keys.

**Cryptanalytic attacks**

Always assume that the attackers know the algorithms used!
(Worst case analysis, and realistic in open systems and standards)

Algorithms should be published anyway, so that their security can be properly evaluated.

Security by _obscurity_ can be very dangerous!

Model of attack -

* Input: Whatever adversary knows from the start (public key, etc.)
* Oracle: Models information adversary can obtain _during_ an attack. (The type of information characterizes the type of attack)
* Output: Whatever the adversary wants to compute, e.g. the secret key, some info from plaintext, etc.

Types of attack -

* Ciphertext Only -
    Given `C1=Ek(M1),...,Cn=Ek(Mn)`, deduce `M1,...,Mn` (or an algorithm to compute `Mn+1` from `Cn+1`)
* Known Plaintext -
    Given `M1,C1=Ek(M1),...,Mn,Cn=Ek(Mn)`, inverse key or algorithm to compute `Mn+1` from `Cn+1`.
* Chosen Plaintext -
    Same as above, but cryptanalyst may choose any message `M1,...,Mn`
* Adaptive Chosen Plaintext -
    Can not only choose plaintext, but modify the plaintext based on encryption results.
* Chosen Ciphertext -
    Can choose different ciphertexts to be decrypted and gets access to decrypted plaintext.

Building a definition of security -

1) Specify an oracle (type of attack)
2) Define what the adversary needs to do to 'win' - what should the output achieve.
3) The system is secure if any _efficient_ algorithm wins the game with only _negligible_ probability.

A standard definition (conventional encryption) -

* No input data for adversary
* Choose plaintext attack of following kind:
  * Case 0 - when asked to encrypt `m`, oracle returns `Ek(m)` under a fixed key k that is chosen randomly initially.
  * Case 1 - oracle returns encryption of _totally random message_, independent of `m`.

In case 1, the adversary gets totally useless data. If they cannot distinguish this from correct encryptions then they cannot do any damage in the real world (case 0) either

Unconditional Security -

System is secure even if an attacker has unbounded computing power since the ciphertext provides insufficient information to uniquely determine corresponding plaintext.

(Security measure with _information theory_)

Apart from a one-time pad (OTP), there is no unconditionally secure encryption algorithm.

Because of this, we should strive instead for an algorithm that meets one or both of:

* Cost of breaking cipher exceeds value of encrypted information.
* Time required to break cipher exceeds useful lifetime of information.

This algorithm is **computationally secure** if either of these is met.

Conditional Security -

(Security measure with _complexity theory_)

System can be broken in principle, but it requires more computing power than an attacker would realistically have.

---
