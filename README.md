# Unicode Pattern Search Engine for Indian Language Wikipedia

A DSA project in Java: a Unicode-aware multilingual search engine (English, Telugu, Hindi, Tamil, Bengali) with a React/Vite frontend. Every core algorithm is hand-written; no Lucene/Elasticsearch/Solr/SQL `LIKE`.

## Project Title
Unicode Pattern Search Engine for Indian Language Wikipedia

## Abstract
This project presents a Unicode-aware multilingual search engine for Indian language text retrieval. The system supports English, Hindi, Telugu, Tamil, and Bengali content with exact matching, multi-keyword queries, and fuzzy typo-tolerant search. The engine uses classical data-structure and algorithmic techniques such as inverted indexing, KMP, Rabin-Karp, Z-algorithm, Aho-Corasick, and Levenshtein distance to provide fast and accurate retrieval.

## Description
The project addresses a common challenge in multilingual information retrieval: standard search methods often fail when text contains Unicode characters, combining marks, or script variations. This repository includes both backend and frontend components and implements a practical search engine that can process a multilingual corpus, normalize text, support case-insensitive matching, and return meaningful snippets from the original documents.

The system supports exact keyword search, multi-keyword AND search, typo-tolerant suggestions, and network-flow analysis for keyword-document relationships. It is designed to demonstrate how Data Structures and Algorithms can be applied to build a real and useful search tool.

## Repository Structure
- [Abstract/README.md](Abstract/README.md) — project abstract
- [Description/README.md](Description/README.md) — full technical description
- [Code/README.md](Code/README.md) — code organization and execution instructions
- [Outputs/README.md](Outputs/README.md) — sample search results and expected output
- [Data/README.md](Data/README.md) — dataset description and corpus information
- [PPTs/README.md](PPTs/README.md) — presentation structure
- [Read me/README.md](Read%20me/README.md) — quick repository overview

## Project Summary
This repository is a complete academic and technical implementation of a multilingual pattern-search engine. It combines algorithmic programming with practical UI design and demonstrates how to retrieve content from Indian-language text corpora reliably and efficiently.

## Main Components
- Backend in Java for indexing, query handling, and matching algorithms
- Frontend in React/Vite for interactive search and results display
- Multilingual corpus under the `corpus/` folder
- Support for exact search, multi-keyword matching, and fuzzy suggestions

## How to Run
```bash
cd backend
mkdir -p bin && javac -encoding UTF-8 -d bin $(find src -name "*.java")
java -cp bin com.unicodesearch.api.ApiServer 8080 ../corpus

cd ../frontend
npm install
npm run dev
```

## Notes
This project is ready for GitHub presentation and includes organized documentation sections to match the requested repository structure.
