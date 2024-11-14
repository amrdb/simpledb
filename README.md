## SimpleDB

My projects solutions for the educational database SimpleDB from the Database Design &
Implementation book (2ed).

- feat(file): file manager block
  statistics ([commit](https://github.com/amrdb/simpledb/commit/4948b9ea0b37703d6af37259be9dc8f18a428d24))
- feat(file): handle data not fitting in
  page ([commit](https://github.com/amrdb/simpledb/commit/c2e44635a5f48d98e758c0760114e499cce0762e))
- feat(file): support boolean data
  type ([commit](https://github.com/amrdb/simpledb/commit/40e87df503bcd5c297ac45237816095a271cf77c))
- feat(buffer): LRU buffer replacement
  strategy ([commit](https://github.com/amrdb/simpledb/commit/118de84d00bf26d85b76f103c2cd6b51d6851d86))
- feat(recovery): fuzzy (non-quiescent)
  checkpoints ([commit](https://github.com/amrdb/simpledb/commit/d29e2c9725d2e68415133f660ec7c2fd1435e46f))
- feat(record): support null values in fields using record's null
  bitmap ([commit](https://github.com/amrdb/simpledb/commit/2d4f589639d844f4f0a4b663832c83520e5bb639))
- feat(tx_concurrency): use wait-die deadlock prevention algorithm
  ([commit](https://github.com/amrdb/simpledb/commit/10a50018723a2fa6897f13840d5694c6a71036c5))

> Note: unit tests are not written using JUnit to follow the author's style.

For general information about simpledb:

- [README.txt](README.txt)
- https://cs.bc.edu/~sciore/simpledb/
