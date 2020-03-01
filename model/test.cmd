@echo off
setlocal

if [%1]==[] (
  echo no test class specified
  echo running all
  gradle test
  goto:eof
)

if [%1]==[index] (
  explorer "build\reports\tests\test\index.html"
  goto:eof
)
if [%1]==[jacoco] (
  gradle report
  explorer "build\jacoco.html\index.html"
  goto:eof
)

gradle test --tests %*
choms