#!/bin/bash

mkdir -p test_files

echo "Creating 10 test files, 100 MB each..."
for i in {1..10}; do
    echo "Creating file_${i}.bin..."
    dd if=/dev/zero of=test_files/file_${i}.bin bs=1M count=100 2>/dev/null
done

echo "Done! Files created in test_files/"
ls -lh test_files/
