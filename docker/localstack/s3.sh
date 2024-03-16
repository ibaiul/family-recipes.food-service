#!/usr/bin/env bash
set -e
export TERM=ansi
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-2
export PAGER=

echo "S3 Configuration started"

aws --endpoint-url=http://localhost:4566 s3 mb s3://test-bucket

echo "S3 Configured"