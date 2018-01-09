# extract-fields

Tools to extract data from unstructured Clojush log files into CSV files

## Installation

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

$ java -jar extract-fields-0.1.0-standalone.jar [args]

Use something like:

```{R}
ns <- syllables %>% group_by(val.type) %>% mutate(ind = row_number()) %>% spread(val.type, value)
```

in R to "spread" out the `val.type` column. If there are unique IDs in each you
you can just call `spread` directly without all this grouping/mutating stuff.

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2018 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
