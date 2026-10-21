create extension if not exists vector;

create table vector_store (
    id        uuid primary key,
    content   text,
    metadata  json,
    embedding vector(1536)
);

create index vector_store_embedding on vector_store using hnsw (embedding vector_cosine_ops);
