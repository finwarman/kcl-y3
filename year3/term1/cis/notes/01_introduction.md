# Lecture 1 - Introduction & What is Information Security

## Assessment -

* Quizzes on keats (weekly) 10%
* Intermediate keats quiz (30%)
* Final written exam (60%)

## Objectives & Learning Outcomes -

To introduce both theoretical and practical (and technological) aspects of cryptography and information security.

### Students should be able to -

* Understand the relevant mathematical techniques associated with cryptography
* Understand the principles of cryptographic techniques & perform implementations of selected algorithms.
* Appreciate the application of security techniques in solving real-life security problems in practical systems.

Complementary & Introductory to other modules re. security.

---

## Syllabus

Basic terminology and concepts

- goals of cryptography, terminology and notations, players
- basic cryptographic functions

Number theory

- congruent modulo n, equivalent class modulo n
- integfer modulo n (Zn)
- multiplicative inverse
- relatively prime
- euler's theorem
- fermat's last theorem
- EEA (Extended Euclidian Algorithm)
- CRY (Chinese Remainder Theorem)

Ciphers

- Block Ciphers (subsistution, tranpositions, product)
- Stream chipers
- Modes of operation (ECB, CBC, CFB, OFB)

Cryptosystems -

- block ciper, DES, AES
- Public-key: RSA, El Gamal
- one-way hash funcion: SHA and MD5
- password hashing and salting

Key Establishment protocls:

- symetrric /asymterric technqiues
- public-key necypriont
- basic and advanced kerberos protocols

Authentication and Identification

- Concepts
- FIatShamir and Feigh Fiat Shamir prootcols
- Zero-knowledge identification procedure

Digital Signatures

- Clasificaiton
- Digital signautre shcemes: rsa, el-gamal, dsa, dss

Informatino Security:

- Passowr Systems
- Intro. to viruses, secure comms, social engineering / phishing, firewall, buffer overflow, DOS

---

### Recommended Reading

(Slides+Lectures will cover all topics)

* William Stallings.Cryptography and Network Security. Principlesand Practice, 7th ed., Prentice Hall, 2016.
* Alfred Menezes, Paul van Oorschot, Scott Vanstone, A.J. Menezes.Handbook of Applied Cryptography, CRC Press, 1996 and 2018).Available online: <http://cacr.uwaterloo.ca/hac/>.
* Wenbo Mao.Modern Cryptography: Theory & Practice, PrenticeHall, 2003.
* Christof Paar and Jan Pelzl.Understanding Cryptography: ATextbook for Students and Practitioners, Springer, 2010.
* Bruce Schneier.Applied Cryptography, John Wiley & Sons, 1996(and 20th anniversary edition in 2016).
* Niels Ferguson, Bruce Schneier, Tadayoshi Kohno.CryptographyEngineering, John Wiley & Sons, 2010

---

## What is information security

* Computer Security = prevention and detection of unauthorized actions by users of a cmoputer system

* Network Security = Provisions made in an underlying computer network infrastructure, policies to protect network+network accessible resources from unauthorized access (and the effectiveness of these methods)

* Information Security (possibly more general) = Dealing with information, independent of computer systems:

* Protecting information and information-systems from unauthorized access, use, disclosure, disruption, or destruction.

---

### Who needs information security

Everyone who works with computers.
Central to software engineers/sys admins, but also for normal citizens.

### Is your data worth protecting

Your personal data is interesting - finance, health, family, religion, politics, hobbies.

Your data is valuable to others - sales departments, future employers, agencies

Information security is therefore required in computing, banking, telecoms, critical infrastructure.

### How do we turn an insecure communication facility (e.g. the internet) into a secure one

One enabling technology is **cryptography**.

---

## What is cryptography

Cryptography: The **science** of secret writing; encryption, description, encoding, ciphers.

Cryptology: The **study** of secret writing.

Steganography: Science of hiding messages in other messages

Cryptanalysis: Science of recovering plaintext from ciphertext wihtout the key.

---

### Agents (aka. principals)

### Naming convention, from security protocols tradition

* Honest Agents - Alice, Bob, Carol,.. (communicating with eachother)

* Dishonest Agents (attackers/intruders) - Eve (Eavesdropper, passive listening attacker), Charlie, Mallory, Zoe (malicious, active attackers)

* Trusted / Neutral - Simon and Trent (trusted servers), Peggy and Victor (prover and verifier - zero-knowledge protocols)

## Tasks -

Q) How can Alice and Bob exchange information securely over an insecure medium, possibly under the control of hacker Charlie?

A) If the message are end-to-end encrypted, then even if the communication channel is compromised, the messages are not known.
The sender encrypts the information, and only the receiver is able to decrypt it.

Either with symmetric encryption (private key) where both parties have the same private key, or asymmetric (public key) where encryption happens with a shared public key, but the plaintext can only be retrieved with a secret private key with the receiving party.

Q) What does it mean for a communication to be secure?

A) Secure means that one or more security properties are guaranteed, e.g. confidentiality/integrity/authentication/anonymity/unobservability/non-repudiation (knowing true source))/availability/etc.)

---

# Security Properties

Common security properties spell out 'CIA":

Confidentiality (secrecy), Integrity, Availibty

* C: No unauthorized access to information
* I: No unauthorized modification of information
* A: No unauthorized impairment of functionality

(Im)proper access to / modification of information be specified for each system.

---

## Confidentiality

_Information is not learned by unauthorized principals_

e.g

```
Alice -------> Bob
          \
            -> Eve (attacker)
```

Confidentiality is guaranteed whenever Eve, who is not authorized to read the message, is *not* able to read the message.

---

Email is not a letter, but a postcard!

* Threat: Everyone can read it along the way.
* Mechanism: Network Security, Encryption, Access Control
* Challenges: Key & Policy Management

---

### Confidentiality, privacy, and anonymity -

In general, concerned with the unauthorized learning of information.
(or considering access control measures)

**Confidentiality** presumes an 'authorized party', or generally a security policy of who can/cannot access our data.

**Privacy** mostly pertains to confidentiality for individuals, whereas secrecy pertains to confidentality for organisations.
Privacy is also used in the sense of anonymity - keeping one's identity private.

* Privacy:  You choose what you let other people know
* Anonymity: Condition where your true identity is not known

---

Encryption dos not hide identities (routing info.), only payload. Even at IP-level.

Routing information is public - each IP packet reveals its identity source and destination. A passive observer can determine who is talking to whom.

The internet is designed as public network - anyone can see traffic going by and read it as it passes. It is up to sender/receiver to encrypt information.

Anonymity set = group within which your actions cannot be distinguished from others in the group.

The large the anonymity set, the better - you cannot be anonymous by yourself!
Anonymising services work best when they attract many users (therefore usability is central to success)

---

### Attacks on anonymity -

passive traffic analysis (infer from whom is talking to whom) - to hide your traffic, you must carry other people's traffic

active traffic analysis - inject packets or put timing signature on packet flow

compromise of network nodes (routers) - traffic can be logged, it is best not to trust any individual node, always assume a fraction of routing nodes are compromised.

---

### Anonymity, unlinkability, unobservability

Anonymity = state of not being identifiable within a set of subjects

* Unlinkability of actions and identity -
e.g. a sender and their message are no more linked after observing communication (even if you know them both separately)

* Unobservability (hard to achieve) -
Observer cannot even tell whether a certain action has taken place or not

---

## Integrity

__Data has not been (maliciously) altered__

In computer security, we are concerned with preventing the (possibly malicious) alteratin of data, by someone who is unauthorized to do so.

Integrity here can be characterized at the unauthorised _writing of data_, assuming a policy of saying who can/cannot alter data

---

## Availability

__Data/Services can be accessed when required__

It's important services can be accessed in a reliable and timely way -

threats to this include:
external environmental events (power loss, fire, etc.)
accidental/malicious software attacks (e.g. infecting system with debilitating virus)

Ensuring availability = preventing denial of server (DoS) attack to the extent this is possible.

Fixing faulty protocols etc. is possible, but much harder to prevent attacks exhausting available resources since it's hard to distinguish attacker for a legitimate use of the service.

e.g. DDoS attacks on websites, interfering with IP routing.

* Example: Communication with a server
* Threats: DoS, break-ins, etc.
* Mechanism: Fire-walls, virus-scanners, backups, redundant hardware, secure OS, etc.
* Challenges: Difficult to cover all threats, especially while still having a usable system.

---

## Accountability

__Actions can be traced to responsible principals__

```
* Alice: "I sent the message"
* Bob:   "I recieved the message"
```

Actions are recorded and can be traced to the party responsible -

If prevention methods & access controls fails, we may fall back to detecting.
Keeping a secure audit trail is important so actions affecting security can be traced back to the responsible party.

Difficult problem: if machine is compromised, logs can also be tampered with. Keep read-only on a separate system.

---

## Non-Repudiation

Strong form of accountability is non-repudiation, when a party cannot later deny some action.

Actions done cannot be denied

---

## Authentication

__Principals or data origin can be identified accurately__

Data or services available only to authorised identities

This is verification of identity of a person or system -
some form of authentication is a pre-requisite for allowing access to some and denying to others, using access-control systems.

Methods characterised as:

* Something you have (keycard)
* something you know (password, secret key, etc.)
* something you are (fingerprint, signature, biometric, etc.)

Several methods may be combined for extra security, and these things may be explicitly (login screen) or implicitly (session key sent with requests) checked.

**Violation**: purporting to be somebody else (identity theft), IP spoofing, stealing private key & signing documents, etc.

---

## Security is a **whole system** issue

---

## Security mechanisms (or countermeasures)

Consider how different **mechanisms** can be used to achieve **goals** in the face of **threats**, and what some of the **challenges** are.

Protection Countermeasures -

* Prevention
* Detection
* Response

---

### Conclusion -

* Security is an enabling technology
* Security is power
* Security is multi-disciplinary

(Crypto., Networks, Software Eng, OPerating Systems, FOrmal methods, law, social sciences, psychology, digital humanities, business processes, etc.)
