# Code

## Overview
The project code is divided into backend and frontend components.

## Backend
The backend is implemented in Java and contains:

- search engine logic
- algorithms for pattern matching
- Unicode normalization and preprocessing
- indexing and ranking components
- API server for queries

### Main Backend Areas
- `backend/src/main/java/com/unicodesearch/algorithm` — pattern-matching algorithms
- `backend/src/main/java/com/unicodesearch/index` — inverted index and corpus loading
- `backend/src/main/java/com/unicodesearch/service` — business logic and search operations
- `backend/src/main/java/com/unicodesearch/api` — HTTP server and endpoints
- `backend/src/test/java/com/unicodesearch` — project tests

## Frontend
The frontend is implemented using React and Vite.

### Main Frontend Areas
- `frontend/src/` — UI components and application logic
- `frontend/public/` — static assets
- `frontend/package.json` — project dependencies and scripts

## Run Instructions
### Backend
```bash
cd backend
mkdir -p bin
javac -encoding UTF-8 -d bin $(find src -name "*.java")
java -cp bin com.unicodesearch.api.ApiServer 8080 ../corpus
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

## Notes
This project is a DSA-based implementation and focuses on algorithmic correctness and multilingual matching rather than using external search engine libraries.
