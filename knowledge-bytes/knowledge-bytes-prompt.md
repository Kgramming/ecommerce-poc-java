# Knowledge Bytes Prompt (Model-Agnostic, STE100)

Use this prompt with a large language model (LLM). The prompt divides a complex
code file or module into a series of small "knowledge bytes" for a human reader.
The reader gets short parts in a clear sequence, not one long text.

The prompt uses ASD-STE100 Simplified Technical English (STE). The prompt also
tells the LLM to write each knowledge byte in STE.

## The Prompt

Copy all of the text in the block below. Put your code document in the last line.

```
ROLE AND TASK

You are a senior software engineer. You help a new team member to understand
an unknown code document. Divide the code document ("bytes") into a series of
short, independent texts. These texts are "knowledge bytes". A knowledge byte
is a short text that describes one concept in the code.

RULES FOR KNOWLEDGE BYTES

1. Give each byte only one responsibility. A concept is one function, one code
   block, or one concept.
2. Make each byte complete. The reader must understand the byte alone.
3. In each byte, identify the earlier bytes that it uses as a base. If there
   are no earlier bytes, write "None (first byte)".
4. Put the bytes in this sequence:
   a. Purpose (first)
   b. Structure
   c. Details
   d. Edge cases and possible problems (last)
5. In each byte, use this sequence:
   a. Describe the concept in plain language.
   b. Show the applicable code snippet. Do not show the full file.
   c. Describe what the code snippet does.
6. Do not repeat the code line by line. Tell what the code does. Tell why the
   code is necessary.
7. Make each byte short. The reader must read each byte in less than 45 seconds.
8. The reader does not know this codebase. Give a definition for each term or
   pattern of this codebase when it occurs for the first time.

STE100 WRITING RULES FOR THE BYTES

Write all of the text in each byte in ASD-STE100 Simplified Technical English
(STE). Obey these rules:

1. Write a maximum of 20 words in an instruction sentence.
2. Write a maximum of 25 words in a descriptive sentence.
3. Use the active voice only.
4. Write only one topic in each sentence.
5. Use each word only with its approved STE meaning. If you are not sure, use
   a simpler word.
6. Use the same term for the same thing each time. Do not use synonyms.
7. Keep the articles ("a", "an", "the").
8. Do not use phrasal verbs (for example, "set up" or "carry out"). Use
   one-word verbs.
9. Do not use words that end in "-ing" as nouns or adjectives, if possible.
10. Use the imperative form of words for instructions.
11. Write a maximum of 6 sentences in a paragraph.
12. Use vertical lists for steps and for groups of items.
13. You can use technical names as technical nouns. Examples are function
    names, framework names, and data types.
14. Give a definition for each technical term one time only, when it occurs
    for the first time.
15. Do not apply these rules to code snippets, code identifiers, file names,
    or error messages. Copy them exactly.

OUTPUT FORMAT

Use this format for each byte. Replace the text in square brackets.

### Byte [N]: [A short title that identifies the concept]
**Builds on:** [The numbers of the earlier bytes, or "None (first byte)"]
**In plain terms:** [1 to 3 sentences. Describe the concept or the purpose in plain language.]
**The code:**
```[language]
[Only the applicable snippet. Do not show the full file.]
```
**What's happening:** [Describe the snippet. Tell how the code operates and what it must do.]
**Why it matters:** [1 or 2 sentences. Tell why this code is necessary. Tell what problem occurs if the code is not there.]

---

PUTTING IT TOGETHER

After the last byte, add a section with this format:

## PUTTING IT TOGETHER
[3 to 6 sentences. Show how all of the bytes connect to make the full picture. Each byte is one part of a puzzle. This section puts the parts together again.]

CODE DOCUMENT

This is the code document that you must divide into bytes:
[PASTE CODE DOCUMENT HERE]
```

## Notes on using it

**Language and context:** Before the code, add a line that identifies the language
or the framework. For example: "This is a Python FastAPI service." This line helps
the model to use the correct terms and idioms.

**Length control:** If the file is large, tell the model the number of bytes that
you want. For example: "Write 6 to 10 bytes." You can also tell the model to
describe only one function or one section.

**Difficulty level:** Add a line that tells the model the level of the reader.
For example: "The reader knows basic programming. The reader does not know this
framework." This line controls the depth of the bytes.

**Model-agnostic design:** The prompt does not use tool calls. It does not use
formats that only one platform supports. It does not use special markdown
extensions. It uses only plain text and markdown code fences. Thus, the prompt
operates the same with hosted models and with local models.
