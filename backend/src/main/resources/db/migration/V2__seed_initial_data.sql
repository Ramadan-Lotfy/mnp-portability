INSERT INTO operators (code, name) VALUES
    ('vodafone', 'Vodafone'),
    ('etisalat', 'Etisalat'),
    ('orange',   'Orange');

INSERT INTO number_ranges (operator_id, range_start, range_end)
SELECT id, '01000000000', '01099999999' FROM operators WHERE code = 'vodafone';

INSERT INTO number_ranges (operator_id, range_start, range_end)
SELECT id, '01100000000', '01199999999' FROM operators WHERE code = 'etisalat';

INSERT INTO number_ranges (operator_id, range_start, range_end)
SELECT id, '01200000000', '01299999999' FROM operators WHERE code = 'orange';