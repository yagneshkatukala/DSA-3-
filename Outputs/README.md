# Outputs

## Sample Search Behavior
The engine is designed to return relevant documents for query terms across multiple languages. Some sample behaviors include:

- exact search for a keyword in the corpus
- multi-word search with AND logic
- typo-tolerant fuzzy search for misspelled inputs
- matched snippets displayed from the original document
- document ranking based on match quality

## Example Outputs
- Query: `india` → relevant documents containing the term
- Query: `science and technology` → results that satisfy the multi-keyword search
- Misspelled query: `computr` → suggested correction such as `computer`
- Multiple keywords: `india water` → documents containing both terms

## Result Format
The output includes:

- document ID
- document title
- matching score or match summary
- snippet text
- number of matches

## Practical Purpose
These outputs show how the system can be used to search multilingual documents in a structured and explainable way.
