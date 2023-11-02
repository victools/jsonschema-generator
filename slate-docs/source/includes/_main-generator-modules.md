# Generator – Modules
Similar to an `OptionPreset` being a short-cut to including various `Option`s, the concept of `Module`s is a convenient way of including multiple [individual configurations](#generator-individual-configurations) or even [advanced configurations](#generator-advanced-configurations) (as per the following sections) at once.

You can easily group your own set of configurations into a `Module` if you wish.
However, the main intention behind `Module`s is that they are an entry-point for separate external dependencies you can "plug-in" as required via `SchemaGeneratorConfigBuilder.with(Module)`, like the few standard `Module`s documented below.

<aside class="notice">
    There may be other available modules outside of this repository.
    Refer to the main <a href="https://github.com/victools/jsonschema-generator/blob/master/README.md">README</a> for the list of known modules.
</aside>
