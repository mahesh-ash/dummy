// Universal field renderer based on type
const renderField = (fieldName) => {
    const config = getConfig(fieldName);
    
    if (config.hidden) return null;

    // Build validation rules
    const rules = {};
    if (config.mandatory && !config.readOnly && !config.disabled) {
        rules.required = `${config.label} is required`;
    }
    if (config.pattern) {
        rules.pattern = {
            value: new RegExp(config.pattern),
            message: config.patternMessage || 'Invalid format',
        };
    }
    if (config.min !== undefined) {
        rules.min = {
            value: config.min,
            message: `${config.label} cannot be less than ${config.min}`,
        };
    }

    // Render based on type
    switch (config.type) {
        case 'async-typeahead':
            if (fieldName === 'acc_id') {
                return (
                    <FormField
                        key={fieldName}
                        control={form.control}
                        name={fieldName}
                        rules={rules}
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel className={`gap-1 font-medium ${config.className}`}>
                                    {config.label}
                                    {config.mandatory && <span className="text-red-500">*</span>}
                                </FormLabel>
                                <div style={{ position: 'relative' }}>
                                    <AsyncTypeahead
                                        id={fieldName}
                                        labelKey="account_name"
                                        options={accountOptions}
                                        isLoading={isLoading}
                                        minLength={1}
                                        placeholder={config.placeholder}
                                        onSearch={handleAccountSearch}
                                        onChange={handleAccountChange}
                                        selected={selectedAccount}
                                        disabled={config.disabled || !!editingRow}
                                        inputProps={{
                                            className: `default-input !h-[40px] dark:placeholder-grey-100 ${config.className}`,
                                            style: { pointerEvents: 'auto' },
                                            required: config.mandatory,
                                        }}
                                        menuStyle={{
                                            zIndex: 1050,
                                            width: '100%',
                                            position: 'absolute',
                                        }}
                                        renderMenu={renderAccountMenu}
                                    />
                                    <div className="min-h-[20px]">
                                        <FormMessage />
                                    </div>
                                </div>
                            </FormItem>
                        )}
                    />
                );
            } else if (fieldName === 'ticker') {
                return (
                    <FormField
                        key={fieldName}
                        control={form.control}
                        name={fieldName}
                        rules={rules}
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel className={`gap-1 font-medium text-sm ${config.className}`}>
                                    {config.label}
                                    {config.mandatory && <span className="text-red-500">*</span>}
                                </FormLabel>
                                <div>
                                    <AsyncTypeahead
                                        id={fieldName}
                                        labelKey={(option) => option.ticker_symbol}
                                        options={tickerOptions}
                                        isLoading={isTickerLoading}
                                        minLength={1}
                                        placeholder={config.placeholder}
                                        onSearch={handleTickerSearch}
                                        onChange={handleTickerChange}
                                        selected={selectedTicker}
                                        disabled={config.disabled}
                                        inputProps={{
                                            className: `flex h-10 w-full rounded-md border border-input bg-background px-4 py-2 text-sm sm:text-base ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-0 disabled:cursor-not-allowed disabled:bg-blue-100 disabled:opacity-50 ${config.className}`,
                                            maxLength: config.maxLength || 32,
                                        }}
                                        menuStyle={{
                                            zIndex: 1050,
                                            width: '100%',
                                            position: 'absolute',
                                        }}
                                        renderMenu={renderTickerMenu}
                                    />
                                    <div className="min-h-[20px]">
                                        <FormMessage />
                                    </div>
                                </div>
                            </FormItem>
                        )}
                    />
                );
            }
            break;

        case 'date':
            return (
                <FormField
                    key={fieldName}
                    control={form.control}
                    name={fieldName}
                    rules={rules}
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel className={`gap-1 font-medium ${config.disabled ? 'opacity-50' : ''} ${config.className}`}>
                                {config.label}
                                {config.mandatory && <span className="text-red-500">*</span>}
                            </FormLabel>
                            <div>
                                <Popover open={open} onOpenChange={setOpen}>
                                    <PopoverTrigger asChild>
                                        <Button
                                            variant="outline"
                                            disabled={config.disabled}
                                            className={
                                                'w-full justify-start text-left h-10 ' +
                                                'flex rounded-md border border-input bg-background px-4 py-2 ring-offset-background ' +
                                                'focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-0 ' +
                                                (field.value ? '' : ' text-muted-foreground') +
                                                (config.disabled ? ' opacity-50 cursor-not-allowed bg-blue-100' : '') +
                                                ` ${config.className}`
                                            }
                                        >
                                            <CalendarIcon className="mr-2 h-4 w-4" />
                                            {field.value ? formatDateTime(field.value) : <span>{config.placeholder || 'Pick a date'}</span>}
                                        </Button>
                                    </PopoverTrigger>
                                    {!config.disabled && (
                                        <PopoverContent className="w-auto p-0">
                                            <Calendar
                                                mode="single"
                                                selected={field.value ? field.value : undefined}
                                                onSelect={(date) => {
                                                    if (!date) {
                                                        field.onChange('');
                                                        return;
                                                    }
                                                    const formattedDate = format(date, 'yyyy-MM-dd');
                                                    field.onChange(formattedDate);
                                                    setOpen(false);
                                                }}
                                                disabled={isFutureDate}
                                                initialFocus
                                            />
                                        </PopoverContent>
                                    )}
                                </Popover>
                                <div className="min-h-[20px]">
                                    <FormMessage />
                                </div>
                            </div>
                        </FormItem>
                    )}
                />
            );

        case 'select':
            return (
                <FormField
                    key={fieldName}
                    control={form.control}
                    name={fieldName}
                    rules={rules}
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel className={`gap-1 font-medium ${config.syndicateOnly ? 'text-sm' : ''} ${config.className}`}>
                                {config.label}
                                {config.mandatory && <span className="text-red-500">*</span>}
                            </FormLabel>
                            <div>
                                <Select
                                    value={field.value}
                                    onValueChange={field.onChange}
                                    disabled={config.disabled || (fieldName === 'product' && !!editingRow)}
                                    required={config.mandatory}
                                >
                                    <SelectTrigger className={`w-full h-10 justify-between dark:disabled:bg-gray-900 disabled:bg-blue-100 dark:bg-inherit text-sm disabled:cursor-not-allowed ${config.className}`}>
                                        <SelectValue placeholder={config.placeholder || `Select ${config.label}`} />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {config.options.map((option) => (
                                            <SelectItem key={option.value} value={option.value}>
                                                {option.label}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                                <div className="min-h-[20px]">
                                    <FormMessage />
                                </div>
                            </div>
                        </FormItem>
                    )}
                />
            );

        case 'number':
            return (
                <FormField
                    key={fieldName}
                    control={form.control}
                    name={fieldName}
                    rules={rules}
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel className={`gap-1 font-medium ${config.syndicateOnly ? 'text-sm' : ''} ${config.className}`}>
                                {config.label}
                                {config.mandatory && <span className="text-red-500">*</span>}
                            </FormLabel>
                            <div>
                                <Input
                                    {...field}
                                    type="number"
                                    disabled={config.disabled || config.readOnly}
                                    onBlur={(e) => {
                                        field.onBlur();
                                        if (e.target.value !== '' && config.step) {
                                            const decimals = config.step === 'any' ? 2 : (config.step.toString().split('.')[1]?.length || 0);
                                            field.onChange(parseFloat(e.target.value).toFixed(decimals));
                                        }
                                    }}
                                    min={config.min}
                                    max={config.max}
                                    step={config.step}
                                    className={`dark:disabled:bg-gray-900 disabled:bg-blue-100 ${config.className}`}
                                    placeholder={config.placeholder}
                                    required={config.mandatory}
                                />
                                <div className="min-h-[20px]">
                                    <FormMessage />
                                </div>
                            </div>
                        </FormItem>
                    )}
                />
            );

        case 'textarea':
            return (
                <FormField
                    key={fieldName}
                    control={form.control}
                    name={fieldName}
                    rules={rules}
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel className={`gap-1 font-medium ${config.className}`}>
                                {config.label}
                                {config.mandatory && <span className="text-red-500">*</span>}
                            </FormLabel>
                            <div>
                                <Textarea
                                    {...field}
                                    id={fieldName}
                                    rows={config.rows || 4}
                                    disabled={config.disabled || config.readOnly}
                                    className={`shadow-none max-h-[150px] disabled:bg-blue-100 min-h-[115px] dark:disabled:bg-gray-900 ${config.className}`}
                                    placeholder={config.placeholder}
                                    required={config.mandatory}
                                />
                                <div className="min-h-[20px]">
                                    <FormMessage />
                                </div>
                            </div>
                        </FormItem>
                    )}
                />
            );

        case 'text':
        default:
            return (
                <FormField
                    key={fieldName}
                    control={form.control}
                    name={fieldName}
                    rules={rules}
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel className={`gap-1 font-medium ${config.className}`}>
                                {config.label}
                                {config.mandatory && <span className="text-red-500">*</span>}
                            </FormLabel>
                            <div>
                                <Input
                                    {...field}
                                    type="text"
                                    disabled={config.disabled || config.readOnly || (fieldName === 'invoice_no' && !!editingRow)}
                                    maxLength={config.maxLength}
                                    className={`disabled:bg-blue-100 dark:disabled:bg-gray-900 ${config.className}`}
                                    placeholder={config.placeholder}
                                    required={config.mandatory}
                                />
                                <div className="min-h-[20px]">
                                    <FormMessage />
                                </div>
                            </div>
                        </FormItem>
                    )}
                />
            );
    }
};

-----------------------------------------------------------------------------
	<form autoComplete="off" className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-4 sm:gap-x-6 p-2 sm:p-4 invoice-form">
  {/* Render main fields dynamically based on order */}
  {mainFields.map(fieldName => {
    return <div key={fieldName}>{renderField(fieldName)}</div>;
  })}

  {/* Syndicate section - conditionally rendered */}
  {watchedStatus === 'syndicate' && syndicateFields.length > 0 && (
    <div className="col-span-1 sm:col-span-2 lg:col-span-3 grid grid-cols-1 gap-0 mt-2 sm:mt-6">
      <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-2 sm:p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4 mb-4">
        {syndicateFields.map(fieldName => renderField(fieldName))}
      </div>
    </div>
  )}

  {/* Attachment and Comments section - keep your existing code */}
  <div className="col-span-1 sm:col-span-2 lg:col-span-3 grid grid-cols-1 lg:grid-cols-2 gap-x-4 sm:gap-x-6">
    {/* Your existing attachment FormField code */}
    {/* ... */}
  </div>
</form>
	  ----------------------------------------------------------------------------------------
